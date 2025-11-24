package http.clients

import cats.effect.IO
import config.AppConfig
import domain.scrapper.CheckInfo
import http.protocol.LinkUpdate
import sttp.client3.{SttpBackend, asStringAlways, emptyRequest}
import sttp.model.StatusCode
import tofu.logging.Logging
import tofu.syntax.logging.LoggingInterpolator

trait BotClient[F[_]] {
  def sendUpdate(update: LinkUpdate): F[Unit]
}

object BotClient{
  private final class Impl(using config: AppConfig, client: SttpBackend[IO, Any], lm: Logging.Make[IO]) extends BotClient[IO]:
    given Logging[IO] = Logging.Make[IO].forService[BotClient[IO]]

    override def sendUpdate(update: LinkUpdate): IO[Unit] =
      for {
        _ <- infoWith"Sending link update" ("link" -> update.url.toString, "update" -> update.toString)
        response <- client.send(
          emptyRequest
            .post(config.botUrl)
            .response(asStringAlways)
        )

        _ <- response.code match
          case StatusCode.Ok => infoWith"Update sent successfully" ("link" -> update.url.toString, "response" -> response.toString)
          case _ => infoWith"Failed to send update" ("link" -> update.url.toString, "response" -> response.toString)
      } yield ()

  def make(using
           AppConfig,
           SttpBackend[IO, Any],
           Logging.Make[IO]
          ): BotClient[IO] = Impl()
}
