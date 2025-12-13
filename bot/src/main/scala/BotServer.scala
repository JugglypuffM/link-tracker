import bot.Scenarios
import canoe.api.*
import cats.effect.{IO, IOApp, Resource}
import config.AppConfig
import http.clients.ScrapperClient
import http.controller.UpdateController
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Router
import service.UpdateService
import sttp.client3.SttpBackend
import sttp.client3.httpclient.cats.HttpClientCatsBackend
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import tofu.logging.Logging
import tofu.syntax.logging.LoggingInterpolator

object BotServer extends IOApp.Simple {
  override def run: IO[Unit] =
    val app: Resource[IO, Unit] = for {
      config <- Resource.eval(AppConfig.load)
      given AppConfig        = config
      given Logging.Make[IO] = Logging.Make.plain[IO]
      given Logging[IO]      = Logging.Make[IO].byName("BotServer")

      given TelegramClient[IO]   <- TelegramClient[IO](config.token)
      given SttpBackend[IO, Any] <- HttpClientCatsBackend.resource[IO]()
      given ScrapperClient[IO] = ScrapperClient.make

      scenarios = Scenarios.make
      _ <- Resource.eval(Bot.polling[IO].follow(scenarios.botScenarios*).compile.drain)
    } yield ()

    app.useForever
}
