package scrapper

import cats.effect.IO
import cats.implicits.catsSyntaxParallelTraverse1
import config.AppConfig
import domain.link.Setting
import domain.scrapper.{CheckResult, LinkUpdate}
import fs2.kafka.{KafkaProducer, ProducerRecord}
import http.clients.GitHubClient
import kafka.UpdateProducer
import repository.SettingRepository
import tofu.logging.Logging
import tofu.syntax.logging.LoggingInterpolator

import java.time.Instant
import scala.concurrent.duration.*
import scala.util.control.NoStackTrace

trait Scrapper[F[_]] {
  def start: IO[Unit]
}

object Scrapper {
  final private class Impl(using
      config: AppConfig,
      linkRepository: SettingRepository[IO],
      gitHubClient: GitHubClient[IO],
      updateProducer: UpdateProducer[IO],
      lm: Logging.Make[IO]
  ) extends Scrapper[IO] {
    given Logging[IO] = Logging.Make[IO].forService[Scrapper[IO]]

    private val gitHubRepoRegex = """https?://github\.com/([^/]+)/([^/]+)""".r

    private def sendUpdate(update: LinkUpdate): IO[Unit] =
      for {
        _ <- info"Sending update to ${update.ownerId} about ${update.url.toString}: ${update.description}"

        _ <- updateProducer.produce(update)
      } yield ()

    private def processSettingCheck(setting: Setting): IO[Unit] =
      for {
        _ <- info"Checking updates for setting ${setting.link.toString}"

        result <- setting.link.toString match
          case gitHubRepoRegex(owner, repo) => gitHubClient.getRepoInfo(owner, repo)
          case _ =>
            error"Unexpected setting domain for setting ${setting.link.toString}"
              >> IO.raiseError(new Throwable("Unexpected setting domain for setting"))

        _ <- info"Check complete for setting ${setting.link.toString} with result ${result.toString}"

        update = LinkUpdate(setting.ownerId, setting.link, result.toDescription)

        _ <- if (result.lastUpdate.isAfter(setting.lastUpdatedAt)) sendUpdate(update) else IO.unit

        _ <- info"Actualizing setting update data"

        _ <- linkRepository.update(setting.id, result.lastUpdate)
      } yield ()

    override def start: IO[Unit] =
      def loop: IO[Unit] =
        for {
          settings <- linkRepository.allUniqueSettings

          _ <- settings.parTraverse(processSettingCheck)
          _ <- IO.sleep(60.seconds)
          _ <- loop
        } yield ()

      loop
  }

  def make(using
      AppConfig,
      SettingRepository[IO],
      GitHubClient[IO],
      UpdateProducer[IO],
      Logging.Make[IO]
  ): Scrapper[IO] = Impl()
}
