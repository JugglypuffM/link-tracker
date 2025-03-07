package bot

import bot.Commands.*
import bot.Replies.*
import bot.ScrapperClient.{BadRequestException, LinkNotFoundException}
import canoe.api.*
import canoe.methods.messages.SendMessage
import canoe.models.Chat
import canoe.models.messages.TextMessage
import canoe.syntax.*
import cats.effect.IO
import domain.{AddLinkRequest, LinkUpdate, RemoveLinkRequest}
import sttp.client3.UriContext
import sttp.model.Uri
import tofu.logging.Logging
import tofu.syntax.logging.LoggingInterpolator

trait Scenarios[F[_]] {
  def botScenarios: List[Scenario[F, Unit]]
  def sendUpdate(id: Long, update: LinkUpdate): IO[Unit]
}

object Scenarios {
  final private class Impl(scrapper: ScrapperClient[IO])(using c: TelegramClient[IO], lm: Logging.Make[IO])
    extends Scenarios[IO] {
    given Logging[IO] = Logging.Make[IO].forService[Scenarios[IO]]

    private val githubRegex        = """https?://github\.com/[^/]+/[^/]+""".r
    private val stackOverflowRegex = """https?://stackoverflow\.com/questions/\d+/.+""".r

    private def start: Scenario[IO, Unit] =
      for {
        chat   <- Scenario.expect(command(START).chat)
        _      <- Scenario.eval(infoWith"Handling command" ("chat-id" -> chat.id, "command" -> START))
        result <- Scenario.eval(scrapper.registerChat(chat.id).attempt)
        _ <- result match {
          case Right(_) =>
            Scenario.eval(
              infoWith"Succeeded" ("chat-id" -> chat.id, "command" -> START) >> chat.send(REGISTRATION_SUCCESS)
            )
          case Left(_: BadRequestException) =>
            Scenario.eval(
              errorWith"Failed on scrapper" ("chat-id" -> chat.id, "command" -> START) >> chat.send(UNEXPECTED_ERROR)
            )
          case Left(err) =>
            Scenario.eval(
              errorCauseWith"Failed unexpectedly" (err)("chat-id" -> chat.id, "command" -> START) >> chat.send(
                UNEXPECTED_ERROR
              )
            )
        }
      } yield ()

    private def help: Scenario[IO, Unit] =
      for {
        chat <- Scenario.expect(command(Commands.HELP).chat)
        _    <- Scenario.eval(infoWith"Handling command" ("chat-id" -> chat.id, "command" -> Commands.HELP))
        _    <- Scenario.eval(chat.send(Replies.HELP))
      } yield ()

    private def askUrl(chat: Chat): Scenario[IO, Uri] =
      for {
        _         <- Scenario.eval(chat.send(ENTER_URL))
        stringUrl <- Scenario.expect(text)
        url <-
          if (githubRegex.matches(stringUrl) || stackOverflowRegex.matches(stringUrl)) Scenario.pure(uri"$stringUrl")
          else askUrl(chat)
        // Можно еще проверять доступность
      } yield url

    private def track: Scenario[IO, Unit] =
      def askTags(chat: Chat): Scenario[IO, List[String]] =
        for {
          _          <- Scenario.eval(chat.send(ENTER_TAGS))
          stringTags <- Scenario.expect(text).map(_.toLowerCase)
          tags =
            if (stringTags == NO) List.empty
            else stringTags.split(" ").toList
        } yield tags

      def askFilters(chat: Chat): Scenario[IO, List[String]] =
        for {
          _             <- Scenario.eval(chat.send(ENTER_FILTERS))
          stringFilters <- Scenario.expect(text).map(_.toLowerCase)
          filters =
            if (stringFilters == NO) List.empty
            else stringFilters.split(" ").toList
        } yield filters

      for {
        chat    <- Scenario.expect(command(TRACK).chat)
        _       <- Scenario.eval(infoWith"Handling command" ("chat-id" -> chat.id, "command" -> TRACK))
        url     <- askUrl(chat)
        tags    <- askTags(chat)
        filters <- askFilters(chat)
        result  <- Scenario.eval(scrapper.trackLink(chat.id, AddLinkRequest(url, tags, filters)).attempt)
        _ <- result match {
          case Right(_) =>
            Scenario.eval(infoWith"Succeeded" ("chat-id" -> chat.id, "command" -> TRACK) >> chat.send(TRACK_SUCCESS))
          case Left(_: BadRequestException) =>
            Scenario.eval(
              errorWith"Failed on scrapper" ("chat-id" -> chat.id, "command" -> TRACK) >> chat.send(UNEXPECTED_ERROR)
            )
          case Left(err) =>
            Scenario.eval(
              errorCauseWith"Failed unexpectedly" (err)("chat-id" -> chat.id, "command" -> TRACK) >> chat.send(
                UNEXPECTED_ERROR
              )
            )
        }
      } yield ()

    private def untrack: Scenario[IO, Unit] =
      for {
        chat   <- Scenario.expect(command(UNTRACK).chat)
        _      <- Scenario.eval(infoWith"Handling command" ("chat-id" -> chat.id, "command" -> UNTRACK))
        url    <- askUrl(chat)
        result <- Scenario.eval(scrapper.untrackLink(chat.id, RemoveLinkRequest(url)).attempt)
        _ <- result match {
          case Right(_) =>
            Scenario.eval(
              infoWith"Succeeded" ("chat-id" -> chat.id, "command" -> UNTRACK) >> chat.send(UNTRACK_SUCCESS)
            )
          case Left(_: LinkNotFoundException) =>
            Scenario.eval(
              errorWith"Link not found" ("chat-id" -> chat.id, "command" -> UNTRACK) >> chat.send(LINK_NOT_FOUND)
            )
          case Left(_: BadRequestException) =>
            Scenario.eval(
              errorWith"Failed on scrapper" ("chat-id" -> chat.id, "command" -> UNTRACK) >> chat.send(UNEXPECTED_ERROR)
            )
          case Left(err) =>
            Scenario.eval(
              errorCauseWith"Failed unexpectedly" (err)("chat-id" -> chat.id, "command" -> UNTRACK) >> chat.send(
                UNEXPECTED_ERROR
              )
            )
        }
      } yield ()

    private def list: Scenario[IO, Unit] =
      for {
        chat          <- Scenario.expect(command(LIST).chat)
        _             <- Scenario.eval(infoWith"Handling command" ("chat-id" -> chat.id, "command" -> LIST))
        linksResponse <- Scenario.eval(scrapper.getLinkList(chat.id).attempt)
        _ <- linksResponse match {
          case Right(_) =>
            Scenario.eval(infoWith"Succeeded" ("chat-id" -> chat.id, "command" -> LIST) >> chat.send(TRACK_SUCCESS))
          case Left(_: BadRequestException) =>
            Scenario.eval(
              errorWith"Failed on scrapper" ("chat-id" -> chat.id, "command" -> LIST) >> chat.send(UNEXPECTED_ERROR)
            )
          case Left(err) =>
            Scenario.eval(
              errorCauseWith"Failed unexpectedly" (err)("chat-id" -> chat.id, "command" -> LIST) >> chat.send(
                UNEXPECTED_ERROR
              )
            )
        }
      } yield ()

    def sendUpdate(id: Long, update: LinkUpdate): IO[Unit] =
      for {
        msg <- SendMessage(id, LINK_UPDATE(update)).call
        _ <- infoWith"Update sent" ("chat-id" -> id, "link" -> update.url.toString, "description" -> update.description)
      } yield ()

    val botScenarios: List[Scenario[IO, Unit]] =
      List(start, help, track, untrack, list)
  }

  def make(scrapper: ScrapperClient[IO])(using c: TelegramClient[IO], lm: Logging.Make[IO]): Scenarios[IO] =
    Impl(scrapper)
}
