package scrapper

import cats.effect.IO
import cats.implicits.catsSyntaxParallelTraverse1
import config.AppConfig
import domain.link.Setting
import domain.scrapper.CheckResult.GitHubResult
import http.clients.GitHubClient
import metrics.ScrapperMetrics
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
                           metrics: ScrapperMetrics[IO],
                           lm: Logging.Make[IO]
  ) extends Scrapper[IO] {
    given Logging[IO] = Logging.Make[IO].forService[Scrapper[IO]]

    private val gitHubRepoRegex = """https?://github\.com/([^/]+)/([^/]+)""".r

    private def recordMetrics(setting: Setting, resultOpt: Option[GitHubResult], duration: FiniteDuration): IO[Unit] =
      resultOpt match
        case Some(result) if result.lastUpdate.isAfter(setting.lastUpdatedAt) =>
          linkRepository.saveUpdate(setting.link, result.lastUpdate, result.toDescription)
            >> metrics.checkCompleted("success")
            >> metrics.checkDuration("success", duration)
        case Some(_) =>
          metrics.checkCompleted("success")
          >> metrics.checkDuration("success", duration)
        case None =>
          metrics.checkCompleted("failed")
          >> metrics.checkDuration("failed", duration)

    private def processSettingCheck(setting: Setting): IO[Unit] =
      for {
        startTime <- IO.realTime
        resultOpt <- setting.link.toString match
          case gitHubRepoRegex(owner, repo) =>
            gitHubClient.getRepoInfo(owner, repo)
          case _ =>
            error"Unexpected setting domain for setting ${setting.link.toString}"
              >> IO.raiseError(new Throwable("Unexpected setting domain for setting"))

        endTime <- IO.realTime
        duration = (endTime - startTime)
        _ <- recordMetrics(setting, resultOpt, duration)
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
           ScrapperMetrics[IO],
           Logging.Make[IO]
  ): Scrapper[IO] = Impl()
}
