package scrapper

import cats.effect.IO
import cats.implicits.{catsSyntaxParallelTraverse1, toTraverseOps}
import config.AppConfig
import domain.link.{Link, LinkInfo}
import domain.scrapper.{CheckInfo, CheckResult}
import http.clients.GitHubClient
import http.protocol.LinkUpdate
import repository.{ChatRepository, LinkRepository}
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
      chatRepository: ChatRepository[IO],
      linkRepository: LinkRepository[IO],
      gitHubClient: GitHubClient[IO],
      lm: Logging.Make[IO]
  ) extends Scrapper[IO] {
    given Logging[IO] = Logging.Make[IO].forService[Scrapper[IO]]

    private val gitHubRepoRegex = """https?://github\.com/([^/]+)/([^/]+)""".r

    private def checkUpdate(uniqueLink: Link): IO[CheckInfo] =
      for {
        _ <- info"Checking updates for link ${uniqueLink.url.toString}"

        result: CheckResult <- uniqueLink.url.toString match
          case gitHubRepoRegex(owner, repo) => gitHubClient.getRepoInfo(owner, repo)
          case _ =>
            error"Unexpected link domain for link ${uniqueLink.url.toString}"
              >> IO.raiseError(InvalidLinkError())

        _ <- info"Check complete for link ${uniqueLink.url.toString} with result ${result.toString}"
      } yield CheckInfo(uniqueLink, result)

    private def processCheck(update: CheckInfo): IO[LinkUpdate] =
      for {
        infos <- linkRepository.allInfosFor(update.link.url)
        updated = infos.collect {
          case info if info.lastUpdatedAt.exists(date => update.result.lastUpdate.isAfter(date)) => info
        }
        chats = updated.map(_.chatId)
      } yield LinkUpdate(
        update.link.id,
        update.link.url,
        update.result.toDescription,
        chats
      )

    override def start: IO[Unit] =
      def loop: IO[Unit] =
        for {
          links   <- linkRepository.allLinks
          checks  <- links.traverse(link => checkUpdate(link))
          updates <- checks.traverse(processCheck)
          _       <- updates.parTraverse(_ => info"Some update sending logic")
          _       <- IO.sleep(2.seconds)
          _       <- loop
        } yield ()

      loop
  }

  def make(using
      AppConfig,
      ChatRepository[IO],
      LinkRepository[IO],
      GitHubClient[IO],
      Logging.Make[IO]
  ): Scrapper[IO] = Impl()
}
