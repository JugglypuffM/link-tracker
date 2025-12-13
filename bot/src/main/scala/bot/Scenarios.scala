package bot

import bot.Commands.*
import bot.Replies.*
import canoe.api.*
import canoe.models.Chat
import canoe.models.messages.TextMessage
import canoe.syntax.*
import cats.effect.IO
import sttp.client3.UriContext
import sttp.model.Uri
import tofu.logging.Logging
import tofu.syntax.logging.LoggingInterpolator

trait Scenarios[F[_]] {
  def botScenarios: List[Scenario[F, Unit]]
}

object Scenarios {
  final private class Impl(using c: TelegramClient[IO], lm: Logging.Make[IO])
    extends Scenarios[IO] {
    given Logging[IO] = Logging.Make[IO].forService[Scenarios[IO]]

    private val githubRegex = """https?://github\.com/[^/]+/[^/]+""".r

    private def start: Scenario[IO, Unit] =
      for {
        chat <- Scenario.expect(command(START).chat)
        _    <- Scenario.eval(info"Handling start command")
        _    <- Scenario.eval(chat.send("Registration not implemented yet"))
      } yield ()

    private def help: Scenario[IO, Unit] =
      for {
        chat <- Scenario.expect(command(Commands.HELP).chat)
        _    <- Scenario.eval(info"Handling help command")
        _    <- Scenario.eval(chat.send(Replies.HELP))
      } yield ()

    private def track: Scenario[IO, Unit] = {
      def askUrl(chat: Chat): Scenario[IO, Option[Uri]] =
        for {
          _         <- Scenario.eval(chat.send(ENTER_URL))
          stringUrl <- Scenario.expect(text)
          url <-
            if (githubRegex.matches(stringUrl)) Scenario.pure(Some(uri"$stringUrl"))
            else if (stringUrl == TRACK) Scenario.pure(None)
            else askUrl(chat)
        } yield url

      for {
        chat   <- Scenario.expect(command(TRACK).chat)
        _      <- Scenario.eval(info"Handling track command")
        urlOpt <- askUrl(chat)
        _ <- urlOpt match {
          case Some(value) => Scenario.eval(chat.send("Tracking not implemented yet")) >> Scenario.done[IO]
          case None        => Scenario.done[IO]
        }
      } yield ()
    }

    private def untrack: Scenario[IO, Unit] =
      for {
        chat <- Scenario.expect(command(UNTRACK).chat)
        _    <- Scenario.eval(info"Handling untrack command")
        _    <- Scenario.eval(chat.send("Unracking not implemented yet"))
      } yield ()

    private def list: Scenario[IO, Unit] =
      for {
        chat <- Scenario.expect(command(LIST).chat)
        _    <- Scenario.eval(info"Handling list command")
        _    <- Scenario.eval(chat.send("Listing not implemented yet"))
      } yield ()

    val botScenarios: List[Scenario[IO, Unit]] =
      List(start, help, track, untrack, list)
  }

  def make(using TelegramClient[IO], Logging.Make[IO]): Scenarios[IO] =
    Impl()
}
