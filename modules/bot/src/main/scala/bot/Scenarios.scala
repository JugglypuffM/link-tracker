package bot

import bot.Commands.*
import bot.Replies.*
import canoe.api.*
import canoe.methods.messages.SendMessage
import canoe.models.Chat
import canoe.models.messages.TextMessage
import canoe.syntax.*
import cats.effect.IO
import cats.syntax.all.*
import http.clients.ScrapperClient
import ScrapperClient.{BadRequestException, LinkNotFoundException}
import http.protocol.{AddLinkRequest, LinkUpdate, RemoveLinkRequest}
import sttp.client3.UriContext
import sttp.model.Uri
import tofu.logging.Logging
import tofu.syntax.logging.LoggingInterpolator

trait Scenarios[F[_]] {
  def botScenarios: List[Scenario[F, Unit]]
  def sendUpdate(id: Long, update: LinkUpdate): IO[Unit]
}

object Scenarios {
  final private class Impl(using scrapper: ScrapperClient[IO], c: TelegramClient[IO], lm: Logging.Make[IO])
    extends Scenarios[IO] {
    given Logging[IO] = Logging.Make[IO].forService[Scenarios[IO]]

    private val githubRegex = """https?://github\.com/[^/]+/[^/]+""".r

    private def help: Scenario[IO, Unit] =
      for {
        chat <- Scenario.expect(command(Commands.HELP).chat)
        _    <- Scenario.eval(info"Handling ${Commands.HELP} command")
        _    <- Scenario.eval(chat.send(Replies.HELP))
      } yield ()

    private def askUrl(chat: Chat): Scenario[IO, Uri] =
      for {
        _         <- Scenario.eval(chat.send(ENTER_URL))
        stringUrl <- Scenario.expect(text)
        url <-
          if (githubRegex.matches(stringUrl)) Scenario.pure(uri"$stringUrl")
          else askUrl(chat)
      } yield url

    private def track: Scenario[IO, Unit] =
      for {
        chat   <- Scenario.expect(command(TRACK).chat)
        _      <- Scenario.eval(info"Handling ${Commands.TRACK} command")
        url    <- askUrl(chat)
        result <- Scenario.eval(scrapper.trackLink(chat.id, AddLinkRequest(url)).attempt)
        _ <- result match {
          case Right(_) =>
            Scenario.eval(info"Succeeded" >> chat.send(TRACK_SUCCESS))
          case Left(_: BadRequestException) =>
            Scenario.eval(
              error"Failed on scrapper" >> chat.send(UNEXPECTED_ERROR)
            )
          case Left(err) =>
            Scenario.eval(errorCause"Failed unexpectedly" (err) >> chat.send(UNEXPECTED_ERROR))
        }
      } yield ()

    private def untrack: Scenario[IO, Unit] =
      for {
        chat   <- Scenario.expect(command(UNTRACK).chat)
        _      <- Scenario.eval(info"Handling command")
        url    <- askUrl(chat)
        result <- Scenario.eval(scrapper.untrackLink(chat.id, RemoveLinkRequest(url)).attempt)
        _ <- result match {
          case Right(_) =>
            Scenario.eval(
              info"Succeeded" >> chat.send(UNTRACK_SUCCESS)
            )
          case Left(_: LinkNotFoundException) =>
            Scenario.eval(
              error"Link not found" >> chat.send(LINK_NOT_FOUND)
            )
          case Left(_: BadRequestException) =>
            Scenario.eval(
              error"Failed on scrapper" >> chat.send(UNEXPECTED_ERROR)
            )
          case Left(err) =>
            Scenario.eval(errorCause"Failed unexpectedly" (err) >> chat.send(UNEXPECTED_ERROR))
        }
      } yield ()

    private def list: Scenario[IO, Unit] =
      for {
        chat          <- Scenario.expect(command(LIST).chat)
        _             <- Scenario.eval(info"Handling command")
        linksResponse <- Scenario.eval(scrapper.getLinkList(chat.id).attempt)
        _ <- linksResponse match {
          case Right(response) =>
            Scenario.eval(info"Succeeded" >> response.links.traverse_(link => chat.send(LINK_LIST_ENTRY(link))))
          case Left(_: BadRequestException) =>
            Scenario.eval(
              error"Failed on scrapper" >> chat.send(UNEXPECTED_ERROR)
            )
          case Left(err) =>
            Scenario.eval(errorCause"Failed unexpectedly" (err) >> chat.send(UNEXPECTED_ERROR))
        }
      } yield ()

    def sendUpdate(id: Long, update: LinkUpdate): IO[Unit] =
      for {
        msg <- SendMessage(id, LINK_UPDATE(update)).call
        _   <- info"Update ${update.toMsg} sent to $id"
      } yield ()

    val botScenarios: List[Scenario[IO, Unit]] =
      List(help, track, untrack, list)
  }

  def make(using ScrapperClient[IO], TelegramClient[IO], Logging.Make[IO]): Scenarios[IO] =
    Impl()
}
