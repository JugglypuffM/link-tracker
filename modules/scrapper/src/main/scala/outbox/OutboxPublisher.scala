package outbox

import cats.effect.IO
import cats.syntax.all.*
import fs2.Stream
import config.AppConfig
import kafka.UpdateProducer
import repository.ScrapperRepository
import tofu.logging.Logging
import tofu.syntax.logging.*

import scala.concurrent.duration.DurationInt

trait OutboxPublisher[F[_]] {
  def start: Stream[F, Unit]
}

object OutboxPublisher {
  private final class Impl(using
                           config: AppConfig,
                           repo: ScrapperRepository[IO],
                           producer: UpdateProducer[IO],
                           lm: Logging.Make[IO],
  ) extends OutboxPublisher[IO] {
    given Logging[IO] = Logging.Make[IO].forService[OutboxPublisher[IO]]

    private def processBatch: IO[Unit] = for {
      entries <- repo.fetchOutboxPending(config.outbox.batchSize)
      _ <- if (entries.nonEmpty) {
        info"Publishing ${entries.size} outbox events" *>
          entries.parTraverseFilter { update =>
              producer.produce(update)
                .as(Some(update.id))
                .handleErrorWith { err =>
                  error"Failed to publish entry ${update.id}: $err".as(None)
                }
            }
            .flatMap { successIds =>
              if (successIds.nonEmpty)
                repo.markOutboxSent(successIds)
              else IO.unit
            }
      } else info"No updates found"
    } yield ()

    override def start: Stream[IO, Unit] =
      Stream
        .awakeEvery[IO](config.outbox.pollIntervalSeconds.seconds)
        .evalMap(_ => info"Starting update publishing process")
        .evalMap(_ => processBatch)
  }

  def make(using AppConfig, ScrapperRepository[IO], UpdateProducer[IO], Logging.Make[IO]): OutboxPublisher[IO] = Impl()
}
