import cats.effect.*
import config.AppConfig
import domain.link.LinkInfo
import domain.telegram.Chat
import http.clients.{BotClient, GitHubClient, StackOverflowClient}
import http.controller.{LinksController, TgChatController}
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Router
import repository.{ChatRepository, LinkRepository}
import scrapper.Scrapper
import service.{ChatService, LinkService}
import sttp.client3.SttpBackend
import sttp.client3.httpclient.cats.HttpClientCatsBackend
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import tofu.logging.Logging
import tofu.syntax.logging.LoggingInterpolator

object ScrapperServer extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    given Logging.Make[IO] = Logging.Make.plain[IO]

    given Logging[IO] = Logging.Make[IO].byName("ScrapperServer")

    val app: Resource[IO, Unit] = for {
      config <- Resource.eval(AppConfig.load)
      given AppConfig = config

      given Ref[IO, Set[Chat]] <- Resource.eval(Ref.of[IO, Set[Chat]](Set.empty))
      given ChatRepository[IO] = ChatRepository.makeInMemory
      given ChatService[IO]    = ChatService.make

      given Ref[IO, Set[LinkInfo]] <- Resource.eval(Ref.of[IO, Set[LinkInfo]](Set.empty))
      given LinkRepository[IO] <- Resource.eval(LinkRepository.makeInMemory)
      given LinkService[IO] = LinkService.make

      endpoints = LinksController().endpoints ++ TgChatController().endpoints
      swagger   = SwaggerInterpreter().fromServerEndpoints(endpoints, "ScrapperService", "0.0.1")
      routes    = Http4sServerInterpreter[IO]().toRoutes(endpoints ++ swagger)

      server <-
        EmberServerBuilder
          .default[IO]
          .withHost(config.host)
          .withPort(config.port)
          .withHttpApp(Router("/" -> routes).orNotFound)
          .build
          .evalTap(server =>
            info"Server available at http://${config.host.toString}:${config.port.toString}" *>
              info"Swagger available at http://${config.host.toString}:${config.port.toString}/docs"
          )

      given SttpBackend[IO, Any]     <- HttpClientCatsBackend.resource[IO]()
      given GitHubClient[IO] = GitHubClient.make
      given StackOverflowClient[IO] = StackOverflowClient.make
      given BotClient[IO] = BotClient.make
      scrapper = Scrapper.make

      _ <- Resource.eval(scrapper.start)
    } yield ()

    app.useForever
}
