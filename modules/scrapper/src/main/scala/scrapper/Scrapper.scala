package scrapper

import cats.effect.IO
import cats.implicits.catsSyntaxParallelTraverse1
import config.AppConfig
import domain.link.Setting
import domain.scrapper.LinkUpdate
import http.clients.GitHubClient
import repository.SettingRepository
import tofu.logging.Logging
import tofu.syntax.logging.LoggingInterpolator

import scala.concurrent.duration.*
import scala.util.control.NoStackTrace

trait Scrapper[F[_]] {
  def start: IO[Unit]
}

object Scrapper {
  final case class InvalidLinkError() extends Throwable with NoStackTrace

  final private class Impl(using
      config: AppConfig,
      linkRepository: SettingRepository[IO],
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
              >> IO.raiseError(InvalidLinkError())

        _ <- info"Check complete for setting ${setting.link.toString} with result ${result.toString}"

        update = LinkUpdate(setting.ownerId, setting.link, result.toDescription)

        _ <-
          if (result.lastUpdate.isAfter(setting.lastUpdatedAt))
          info"Sending update to ${update.ownerId} about ${update.url.toString}: ${update.description}"
          // Some kafka logic
          else IO.unit

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
      Logging.Make[IO]
  ): Scrapper[IO] = Impl()
}
