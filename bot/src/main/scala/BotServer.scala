import bot.{Scenarios, ScrapperClient}
import canoe.api.*
import cats.effect.{IO, IOApp}
import config.AppConfig
import controller.UpdateController
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

      tgClient = TelegramClient[IO](config.token)

      httpClient     = HttpClientCatsBackend.resource[IO]()
      scrapperClient = ScrapperClient.make(config.scrapperUrl, httpClient)

      _ <- tgClient.use(implicit client =>
        val scenarios = Scenarios.make(scrapperClient)
        val endpoints = UpdateController(scenarios.sendUpdate).endpoints
        val swagger   = SwaggerInterpreter().fromServerEndpoints(endpoints, "LinkUpdateService", "0.0.1")
        val routes    = Http4sServerInterpreter[IO]().toRoutes(endpoints ++ swagger)

        val server = EmberServerBuilder
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

        val bot = Bot.polling[IO].follow(scenarios.botScenarios*).compile.drain

        server &> bot
      )
    } yield ()
}
