package repository.error

import sttp.model.Uri

import scala.util.control.NoStackTrace

case class LinkNotFound(url: Uri) extends Throwable with NoStackTrace
