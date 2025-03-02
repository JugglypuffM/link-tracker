package bot

import canoe.api.*
import canoe.syntax.*
import cats.effect.kernel.Sync

class Commands[F[_]: TelegramClient: Sync](scrapper: ScrapperClient[F]) {
  def start: Scenario[F, Unit] =
    for {
      chat   <- Scenario.expect(command("start").chat)
      result <- Scenario.eval(scrapper.registerChat(chat.id))
      _ <- result match {
        // TODO: возможно распихать ответы по константам
        case Right(_) => Scenario.eval(chat.send("Регистрация успешна!"))
        case Left(_)  => Scenario.eval(chat.send("Произошла непредвиденная ошибка("))
      }
    } yield ()
}
