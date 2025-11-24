package repository.error

import scala.util.control.NoStackTrace

case class ChatNotFound(id: Long) extends Throwable with NoStackTrace
