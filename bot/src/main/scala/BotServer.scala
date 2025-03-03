import bot.{Commands, ScrapperClient}
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

object BotServer extends IOApp.Simple {
  override def run: IO[Unit] =
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
          IO.println(s"Server available at http://${config.host}:${config.port}") *>
            IO.println(s"Swagger available at http://${config.host}:${config.port}/docs")
        )
        .useForever

      bot = Stream
        .resource(TelegramClient[IO](config.token))
        .flatMap(implicit client => Bot.polling[IO].follow(Commands.make(scrapperClient).start))
        .compile
        .drain

      _ <- server &> bot
    } yield ()
}
