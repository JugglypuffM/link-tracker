package bot

import http.protocol.{LinkResponse, LinkUpdate}

object Replies {
  val REGISTRATION_SUCCESS = "Регистрация успешна"

  val HELP: String = """
                 |Доступные команды:
                 |/start - регистрация пользователя
                 |/help - список команд
                 |/track - начать отслеживание ссылки
                 |/untrack - прекратить отслеживание ссылки
                 |/list - список отслеживаемых ссылок
        """.stripMargin

  val TRACK_SUCCESS = "Ссылка успешно добавлена в отслеживаемые"

  val ENTER_URL     = "Введи ссылку на репозиторий Github или вопрос StackOverflow"

  val UNTRACK_SUCCESS = "Ссылка успешна удалена из отслеживаемых"

  val LINK_NOT_FOUND = " Ссылка не найдена"

  val UNEXPECTED_ERROR = "Произошла непредвиденная ошибка"
}
