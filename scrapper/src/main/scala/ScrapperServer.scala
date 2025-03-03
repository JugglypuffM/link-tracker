import cats.effect.{ExitCode, IO, IOApp, Ref}
import com.comcast.ip4s.{Host, Port}
import config.AppConfig
import controller.{LinksController, TgChatController}
import domain.LinkResponse
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Router
import repository.{ChatRepository, InMemoryRepo, LinkRepository}
import service.{ChatService, LinkService}
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir.swagger.bundle.SwaggerInterpreter

object ScrapperServer extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    for {
      config <- AppConfig.load

      repo <- Ref.of[IO, InMemoryRepo](InMemoryRepo(Map.empty, Map.empty, Map.empty))
      chatRepository = ChatRepository.makeInMemory(repo)
      chatService    = ChatService.make(chatRepository)

      linkRepository = LinkRepository.makeInMemory(repo)
      linkService <- LinkService.make(linkRepository)

      endpoints = LinksController(linkService).endpoints ++ TgChatController(chatService).endpoints
      swagger   = SwaggerInterpreter().fromServerEndpoints(endpoints, "ScrapperService", "0.0.1")
      routes    = Http4sServerInterpreter[IO]().toRoutes(endpoints ++ swagger)

      _ <-
        EmberServerBuilder
          .default[IO]
          // TODO: брать с конфига
          .withHost(config.host)
          .withPort(config.port)
          .withHttpApp(Router("/" -> routes).orNotFound)
          .build
          .evalTap(server =>
            IO.println(
              s"Server available at ${config.host}:${config.port}",
              s"Swagger available at ${config.host}:${config.port}/docs"
            )
          )
          .useForever
    } yield ExitCode.Success
}
