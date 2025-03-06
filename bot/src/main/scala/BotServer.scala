import bot.{Scenarios, ScrapperClient}
import canoe.api.*
import cats.effect.{IO, IOApp}
import config.AppConfig
import controller.UpdateController
import fs2.Stream
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Router
import sttp.client3.httpclient.cats.HttpClientCatsBackend
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import tofu.logging.Logging
import tofu.syntax.logging.LoggingInterpolator

object BotServer extends IOApp.Simple {
  override def run: IO[Unit] =
    given Logging.Make[IO] = Logging.Make.plain[IO]

    given Logging[IO] = Logging.Make[IO].byName("BotServer")

    for {
      config <- AppConfig.load

      endpoints = UpdateController().endpoints
      swagger   = SwaggerInterpreter().fromServerEndpoints(endpoints, "LinkUpdateService", "0.0.1")
      routes    = Http4sServerInterpreter[IO]().toRoutes(endpoints ++ swagger)

      httpClient     = HttpClientCatsBackend.resource[IO]()
      scrapperClient = ScrapperClient.make(config.scrapperUrl, httpClient)

      server = EmberServerBuilder
        .default[IO]
        .withHost(config.host)
        .withPort(config.port)
        .withHttpApp(Router("/" -> routes).orNotFound)
        .build
        .evalTap(server =>
          info"Server available at http://${config.host.toString}:${config.port.toString}" *>
            info"Swagger available at http://${config.host.toString}:${config.port.toString}/docs"
        )
        .useForever

      bot = Stream
        .resource(TelegramClient[IO](config.token))
        .flatMap(implicit client => Bot.polling[IO].follow(Scenarios.make(scrapperClient).botScenarios *))
        .compile
        .drain

      _ <- server &> bot
    } yield ()
}
