package bot

import canoe.api.*
import canoe.syntax.*
import cats.effect.IO
import cats.effect.kernel.Sync

trait Commands[F[_]] {
  def start: Scenario[F, Unit]
}

object Commands {
  final private class Impl(scrapper: ScrapperClient[IO])(implicit client: TelegramClient[IO]) extends Commands[IO] {
    def start: Scenario[IO, Unit] =
      for {
        chat   <- Scenario.expect(command("start").chat)
        result <- Scenario.eval(scrapper.registerChat(chat.id).attempt)
        _ <- result match {
          // TODO: возможно распихать ответы по константам
          case Right(_) => Scenario.eval(chat.send("Регистрация успешна!"))
          case Left(_)  => Scenario.eval(chat.send("Произошла непредвиденная ошибка("))
        }
      } yield ()
  }

  def make(scrapper: ScrapperClient[IO])(implicit client: TelegramClient[IO]): Commands[IO] = Impl(scrapper)
}
