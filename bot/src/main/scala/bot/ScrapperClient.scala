package bot

import cats.effect.{IO, Resource}
import sttp.client3.*
import sttp.model.{StatusCode, Uri}

import java.lang.UnknownError

trait ScrapperClient[F[_]] {
  def registerChat(id: Long): F[Either[UnknownError, Unit]]
}

object ScrapperClient {
  final private class Impl(serverUrl: Uri, httpClient: Resource[IO, SttpBackend[IO, Any]]) extends ScrapperClient[IO] {
    def registerChat(id: Long): IO[Either[UnknownError, Unit]] =
      httpClient.use { client =>
        for {
          response <- client.send(
            emptyRequest
              .post(serverUrl.addPath("tg-chat", id.toString))
              .response(asStringAlways)
          )
        } yield response.code match
          case StatusCode.Ok => Right(())
          case _             => Left(UnknownError()) // TODO: логировать ApiErrorResponse
      }
  }

  def make(
      serverUrl: Uri,
      httpClient: Resource[IO, SttpBackend[IO, Any]]
  ): ScrapperClient[IO] =
    Impl(serverUrl, httpClient)
}
