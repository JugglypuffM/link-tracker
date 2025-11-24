package http.clients

import cats.effect.IO
import domain.scrapper.CheckResult.StackOverflow.{StackOverflowAnswer, StackOverflowResult}
import sttp.client3.SttpBackend

trait StackOverflowClient[F[_]] {
  def getAllAnswers(questionId: String): F[StackOverflowResult]
}

object StackOverflowClient{
  private final class Impl(using client: SttpBackend[IO, Any]) extends StackOverflowClient[IO]{

    override def getAllAnswers(questionId: String): IO[StackOverflowResult] = ???
  }
  
  def make(using 
          SttpBackend[IO, Any]
          ): StackOverflowClient[IO] = Impl()
}
