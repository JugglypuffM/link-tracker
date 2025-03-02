import bot.{Commands, ScrapperClient}
import canoe.api.*
import cats.effect.{IO, IOApp}
import com.comcast.ip4s.{Host, Port}
import controller.UpdateController
import fs2.Stream
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Router
import sttp.client3.UriContext
import sttp.client3.httpclient.cats.HttpClientCatsBackend
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir.swagger.bundle.SwaggerInterpreter

object BotServer extends IOApp.Simple {
  override def run: IO[Unit] =
    val endpoints = UpdateController().endpoints
    val swagger   = SwaggerInterpreter().fromServerEndpoints(endpoints, "LinkUpdateService", "0.0.1")
    val routes    = Http4sServerInterpreter[IO]().toRoutes(endpoints ++ swagger)

    val server = EmberServerBuilder
      .default[IO]
      // TODO: брать с конфига
      .withHost(Host.fromString("localhost").get)
      .withPort(Port.fromInt(8080).get)
      .withHttpApp(Router("/" -> routes).orNotFound)
      .build
      .evalTap(server =>
        IO.println(
          s"Server available at http://localhost:${server.address.getPort}"
        )
      )
      .useForever

    val httpClient     = HttpClientCatsBackend.resource[IO]()
    val scrapperClient = ScrapperClient.make(uri"http://localhost:8081", httpClient)

    val bot = Stream
      // TODO: ОБЯЗАТЕЛЬНО УБРАТЬ В КОНФИГ!!!!!!!!1
      .resource(TelegramClient[IO]("nothing interesting here"))
      .flatMap(implicit client => Bot.polling[IO].follow(Commands[IO](scrapperClient).start))
      .compile
      .drain

    server &> bot
}
