package http.clients

import cats.effect.IO
import config.AppConfig
import http.protocol.{AddLinkRequest, LinkListResponse, RemoveLinkRequest}
import sttp.client3.*
import sttp.model.StatusCode
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

  final private class Impl(using config: AppConfig, client: SttpBackend[IO, Any], lm: Logging.Make[IO])
    extends ScrapperClient[IO] {
    given Logging[IO] = Logging.Make[IO].forService[ScrapperClient[IO]]

    def registerChat(id: Long): IO[Unit] =
      for {
        _ <- infoWith"Sending register request" ("chat-id" -> id)
        response <- client.send(
          emptyRequest
            .post(config.scrapperUrl.addPath("tg-chat", id.toString))
            .response(asStringAlways)
        )
        _ <- infoWith"Got response" ("chat-id" -> id, "response" -> response.toString)
      } yield response.code match
        case StatusCode.Ok => ()
        case _             => throw BadRequestException()

    def trackLink(id: Long, request: AddLinkRequest): IO[Unit] =
      for {
        _ <- infoWith"Sending track request" ("chat-id" -> id)

        response <- client.send(
          emptyRequest
            .post(config.scrapperUrl.addPath("links"))
            .header("Tg-Chat-Id", id.toString)
            .body(request.asJson)
            .response(asStringAlways)
        )

        _ <- infoWith"Got response" ("chat-id" -> id, "response" -> response.toString)
      } yield response.code match
        case StatusCode.Ok => ()
        case _             => throw BadRequestException()

    def untrackLink(id: Long, request: RemoveLinkRequest): IO[Unit] =
      for {
        _ <- infoWith"Sending untrack request" ("chat-id" -> id)

        response <- client.send(
          emptyRequest
            .delete(config.scrapperUrl.addPath("links"))
            .header("Tg-Chat-Id", id.toString)
            .body(request.asJson)
            .response(asStringAlways)
        )

        _ <- infoWith"Got response" ("chat-id" -> id, "response" -> response.toString)
      } yield response.code match
        case StatusCode.Ok       => ()
        case StatusCode.NotFound => throw LinkNotFoundException()
        case _                   => throw BadRequestException()

    def getLinkList(id: Long): IO[LinkListResponse] =
      for {
        _ <- infoWith"Sending link list request" ("chat-id" -> id)

        response <- client.send(
          emptyRequest
            .get(config.scrapperUrl.addPath("links"))
            .header("Tg-Chat-Id", id.toString)
            .response(asStringAlways)
        )

        _ <- infoWith"Got response" ("chat-id" -> id, "response" -> response.toString)

        links = response.body.jsonAs[LinkListResponse]
      } yield (response.code, links) match
        case (StatusCode.Ok, Right(value)) => value
        case _                             => throw BadRequestException()
  }

  def make(using
      AppConfig,
      SttpBackend[IO, Any],
      Logging.Make[IO]
  ): ScrapperClient[IO] = Impl()
}
