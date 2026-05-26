import bot.Scenarios
import canoe.api.*
import cats.effect.{IO, IOApp, Resource}
import config.AppConfig
import doobie.Transactor
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import http.clients.ScrapperClient
import kafka.UpdateConsumer
import repository.BotRepository
import sttp.client3.SttpBackend
import sttp.client3.httpclient.cats.HttpClientCatsBackend
import tofu.logging.Logging

object BotServer extends IOApp.Simple {
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
  
  override def run: IO[Unit] =
    val app: Resource[IO, Unit] = for {
      config <- Resource.eval(AppConfig.load)
      given AppConfig        = config
      given Logging.Make[IO] = Logging.Make.plain[IO]
      given Logging[IO]      = Logging.Make[IO].byName("BotServer")

      given TelegramClient[IO]   <- TelegramClient[IO](config.token)
      given SttpBackend[IO, Any] <- HttpClientCatsBackend.resource[IO]()
      given ScrapperClient[IO] = ScrapperClient.make

      given Transactor[IO] <- initTransactor
      given BotRepository[IO] = BotRepository.makeDoobie
      updateConsumer <- UpdateConsumer.make
      _              <- Resource.eval(updateConsumer.listenUpdates.compile.drain.start)

      scenarios = Scenarios.make
      _ <- Resource.eval(Bot.polling[IO].follow(scenarios.botScenarios*).compile.drain.start)
    } yield ()

    app.useForever
}
