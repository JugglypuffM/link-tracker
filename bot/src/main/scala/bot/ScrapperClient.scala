package bot

import cats.effect.{IO, Resource}
import domain.{AddLinkRequest, LinkListResponse, RemoveLinkRequest}
import sttp.client3.*
import sttp.model.{StatusCode, Uri}
import tethys.*
import tethys.jackson.*
import tofu.logging.Logging
import tofu.syntax.logging.LoggingInterpolator

trait ScrapperClient[F[_]] {
  def registerChat(id: Long): F[Unit]

  def trackLink(id: Long, request: AddLinkRequest): F[Unit]

  def untrackLink(id: Long, request: RemoveLinkRequest): F[Unit]

  def getLinkList(id: Long): F[LinkListResponse]
}

object ScrapperClient {
  class BadRequestException extends Exception

  class LinkNotFoundException extends Exception

  final private class Impl(serverUrl: Uri, httpClient: Resource[IO, SttpBackend[IO, Any]])(using lm: Logging.Make[IO])
    extends ScrapperClient[IO] {
    given Logging[IO] = Logging.Make[IO].forService[ScrapperClient[IO]]

    def registerChat(id: Long): IO[Unit] =
      httpClient.use { client =>
        for {
          _ <- infoWith"Sending register request" ("chat-id" -> id)
          response <- client.send(
            emptyRequest
              .post(serverUrl.addPath("tg-chat", id.toString))
              .response(asStringAlways)
          )

          _ <- infoWith"Got response ${response.toString}" ("chat-id" -> id)
        } yield response.code match
          case StatusCode.Ok => ()
          case _             => throw BadRequestException()
      }

    def trackLink(id: Long, request: AddLinkRequest): IO[Unit] =
      httpClient.use { client =>
        for {
          _ <- infoWith"Sending track request" ("chat-id" -> id)

          response <- client.send(
            emptyRequest
              .post(serverUrl.addPath("links"))
              .header("Tg-Chat-Id", id.toString)
              .body(request.asJson)
              .response(asStringAlways)
          )

          _ <- infoWith"Got response ${response.toString}" ("chat-id" -> id)
        } yield response.code match
          case StatusCode.Ok => ()
          case _             => throw BadRequestException()
      }

    def untrackLink(id: Long, request: RemoveLinkRequest): IO[Unit] =
      httpClient.use { client =>
        for {
          _ <- infoWith"Sending untrack request" ("chat-id" -> id)

          response <- client.send(
            emptyRequest
              .delete(serverUrl.addPath("links"))
              .header("Tg-Chat-Id", id.toString)
              .body(request.asJson)
              .response(asStringAlways)
          )

          _ <- infoWith"Got response ${response.toString}" ("chat-id" -> id)
        } yield response.code match
          case StatusCode.Ok       => ()
          case StatusCode.NotFound => throw LinkNotFoundException()
          case _                   => throw BadRequestException()
      }

    def getLinkList(id: Long): IO[LinkListResponse] =
      httpClient.use { client =>
        for {
          _ <- infoWith"Sending link list request" ("chat-id" -> id)

          response <- client.send(
            emptyRequest
              .get(serverUrl.addPath("links"))
              .header("Tg-Chat-Id", id.toString)
              .response(asStringAlways)
          )

          _ <- infoWith"Got response ${response.toString}" ("chat-id" -> id)

          links = response.body.jsonAs[LinkListResponse]
        } yield (response.code, links) match
          case (StatusCode.Ok, Right(value)) => value
          case _                             => throw BadRequestException()
      }
  }

  def make(
      serverUrl: Uri,
      httpClient: Resource[IO, SttpBackend[IO, Any]]
  )(using lm: Logging.Make[IO]): ScrapperClient[IO] =
    Impl(serverUrl, httpClient)
}
