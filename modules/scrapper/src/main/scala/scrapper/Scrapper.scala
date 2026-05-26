package scrapper

import cats.effect.IO
import cats.implicits.catsSyntaxParallelTraverse1
import config.AppConfig
import domain.link.Setting
import http.clients.GitHubClient
import kafka.UpdateProducer
import repository.ScrapperRepository
import tofu.logging.Logging
import tofu.syntax.logging.LoggingInterpolator

import scala.concurrent.duration.*

trait Scrapper[F[_]] {
  def start: IO[Unit]
}

object Scrapper {
  final private class Impl(using
                           config: AppConfig,
                           linkRepository: ScrapperRepository[IO],
                           gitHubClient: GitHubClient[IO],
                           lm: Logging.Make[IO]
  ) extends Scrapper[IO] {
    given Logging[IO] = Logging.Make[IO].forService[Scrapper[IO]]

    private val gitHubRepoRegex = """https?://github\.com/([^/]+)/([^/]+)""".r

    private def processSettingCheck(setting: Setting): IO[Unit] =
      for {
        _ <- info"Checking updates for setting ${setting.link.toString}"

        result <- setting.link.toString match
          case gitHubRepoRegex(owner, repo) => gitHubClient.getRepoInfo(owner, repo)
          case _ =>
            error"Unexpected setting domain for setting ${setting.link.toString}"
              >> IO.raiseError(new Throwable("Unexpected setting domain for setting"))

        _ <- info"Check complete for setting ${setting.link.toString} with result ${result.toString}"

        _ <-
          if (result.lastUpdate.isAfter(setting.lastUpdatedAt))
            linkRepository.saveUpdate(setting.link, result.lastUpdate, result.toDescription)
          else IO.unit
      } yield ()

    override def start: IO[Unit] =
      def loop: IO[Unit] =
        for {
          _ <- info"Starting update checking process"
          settings <-
            linkRepository.getBatch(100, config.scrapper.linkProcessMilliseconds, config.scrapper.checkIntervalSeconds)
          _ <- settings.parTraverse(processSettingCheck)
          _ <-
            if (settings.isEmpty)
              info"No updates found - sleeping for ${config.scrapper.checkIntervalSeconds} seconds" *>
                IO.sleep(config.scrapper.checkIntervalSeconds.seconds)
            else IO.unit
          _ <- loop
        } yield ()

      loop
  }

  def make(using
           AppConfig,
           ScrapperRepository[IO],
           GitHubClient[IO],
           Logging.Make[IO]
  ): Scrapper[IO] = Impl()
}
