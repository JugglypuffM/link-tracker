package repository.error

import scala.util.control.NoStackTrace

case class SaveError() extends Throwable with NoStackTrace
