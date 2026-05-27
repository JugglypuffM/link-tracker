import cats.effect.*
import config.AppConfig
import config.resilience.ResilienceConfig
import doobie.Transactor
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import http.clients.GitHubClient
import http.controller.LinksController
import kafka.UpdateProducer
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Router
import outbox.OutboxPublisher
import resilience.Resilience
import repository.ScrapperRepository
import scrapper.Scrapper
import service.LinkService
import sttp.client3.SttpBackend
import sttp.client3.httpclient.cats.HttpClientCatsBackend
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import tofu.logging.Logging
import tofu.syntax.logging.LoggingInterpolator

object ScrapperServer extends IOApp {
  private def initTransactor(using config: AppConfig): Resource[IO, HikariTransactor[IO]] =
    for {
      ec <- ExecutionContexts.fixedThreadPool[IO](32)
      xa <- HikariTransactor.newHikariTransactor[IO](
        driverClassName = "org.postgresql.Driver",
        url = config.database.url,
        user = config.database.username,
        pass = config.database.password,
        connectEC = ec,
      )
    } yield xa

  override def run(args: List[String]): IO[ExitCode] =
    given Logging.Make[IO] = Logging.Make.plain[IO]

    given Logging[IO] = Logging.Make[IO].byName("ScrapperServer")

    val app: Resource[IO, Unit] = for {
      config <- Resource.eval(AppConfig.load)
      given AppConfig = config

      given Transactor[IO] <- initTransactor

      given ScrapperRepository[IO] = ScrapperRepository.makeDoobie
      given LinkService[IO]       = LinkService.make

      endpoints = LinksController().endpoints
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

      given SttpBackend[IO, Any] <- HttpClientCatsBackend.resource[IO]()
      given Resilience <- Resource.eval(Resilience.make(config.resilience))
      given GitHubClient[IO] = GitHubClient.make
      scrapper = Scrapper.make

      given UpdateProducer[IO] <- UpdateProducer.make
      outbox = OutboxPublisher.make

      _ <- Resource.eval(scrapper.start.start)
      _ <- Resource.eval(outbox.start.compile.drain.start)
    } yield ()

    app.useForever
}
