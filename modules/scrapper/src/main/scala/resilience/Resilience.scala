package resilience

import cats.effect.IO
import cats.effect.Ref
import cats.syntax.applicativeError.*
import config.resilience.ResilienceConfig
import domain.errors.HttpClientError
import domain.errors.HttpClientError.*

import java.util.concurrent.TimeoutException
import scala.concurrent.duration.FiniteDuration

class Resilience private (
    config: ResilienceConfig,
    state: Ref[IO, CircuitState]
) {

  def execute[A](fa: IO[A]): IO[A] =
    withCircuitBreaker {
      retryLoop(
        fa.timeout(config.timeout).adaptError { case e: TimeoutException => TimedOut(e) },
        attempt = 0
      )
    }

  private def retryLoop[A](fa: IO[A], attempt: Int): IO[A] =
    fa.handleErrorWith {
      case e: Transient if attempt < config.maxRetries - 1 =>
        IO.sleep(config.retryDelay) >> retryLoop(fa, attempt + 1)
      case e =>
        IO.raiseError(e)
    }

  private def withCircuitBreaker[A](fa: IO[A]): IO[A] =
    for {
      now <- IO.realTime.map(_.toMillis)
      allowed <- state.modify {
        case s @ CircuitState.Open(until) if now < until => (s, false)
        case CircuitState.Open(_) => (CircuitState.HalfOpen, true)
        case s => (s, true)
      }
      result <- if (allowed) attemptCall(fa) else IO.raiseError(CircuitOpen)
    } yield result

  private def attemptCall[A](fa: IO[A]): IO[A] =
    fa
      .flatTap(_ => state.set(CircuitState.Closed()))
      .handleErrorWith {
        case e: Transient => recordFailure >> IO.raiseError(e)
        case e => IO.raiseError(e)
      }

  private def recordFailure: IO[Unit] =
    for {
      now <- IO.realTime.map(_.toMillis)
      _ <- state.modify {
        case CircuitState.Closed(n) if n + 1 >= config.failureThreshold =>
          (CircuitState.Open(now + config.resetTimeout.toMillis), ())
        case CircuitState.Closed(n) =>
          (CircuitState.Closed(n + 1), ())
        case _ =>
          (CircuitState.Open(now + config.resetTimeout.toMillis), ())
      }
    } yield ()
}

object Resilience {
  def make(config: ResilienceConfig): IO[Resilience] =
    Ref.of[IO, CircuitState](CircuitState.initial).map(new Resilience(config, _))
}
