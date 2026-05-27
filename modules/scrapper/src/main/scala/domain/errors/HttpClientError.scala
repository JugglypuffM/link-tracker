package domain.errors

import scala.concurrent.duration.FiniteDuration
import scala.util.control.NoStackTrace

sealed trait HttpClientError extends Throwable with NoStackTrace

object HttpClientError {
  sealed trait Transient extends HttpClientError

  final case class RateLimited(retryAfter: Option[FiniteDuration]) extends Transient {
    override def getMessage: String = s"Rate limited${retryAfter.map(d => s", retry after ${d.toMillis}ms").getOrElse("")}"
  }

  final case class ServerError(code: Int, body: String) extends Transient {
    override def getMessage: String = s"Server error $code: $body"
  }

  final case class TimedOut(cause: Throwable) extends Transient {
    override def getMessage: String = s"Request timed out: ${cause.getMessage}"
  }

  final case class ConnectionFailed(cause: Throwable) extends Transient {
    override def getMessage: String = s"Connection failed: ${cause.getMessage}"
  }

  sealed trait Permanent extends HttpClientError

  final case class NotFound(url: String) extends Permanent {
    override def getMessage: String = s"Not found: $url"
  }

  final case class Unauthorized(body: String) extends Permanent {
    override def getMessage: String = s"Unauthorized: $body"
  }

  final case class BadRequest(body: String) extends Permanent {
    override def getMessage: String = s"Bad request: $body"
  }

  final case class DeserializationFailed(cause: Throwable) extends Permanent {
    override def getMessage: String = s"Deserialization failed: ${cause.getMessage}"
  }

  final case class UnexpectedStatus(code: Int, body: String) extends Permanent {
    override def getMessage: String = s"Unexpected status $code: $body"
  }

  sealed trait Infrastructure extends HttpClientError

  case object CircuitOpen extends Infrastructure {
    override def getMessage: String = "Circuit breaker is open"
  }

  def fromStatusCode(
      code: Int,
      body: String,
      retryAfterHeader: Option[String]
  ): HttpClientError = {
    val retryAfter = retryAfterHeader.flatMap(h => scala.util.Try(h.toLong).toOption.map(FiniteDuration(_, "seconds")))
    code match {
      case 429 => RateLimited(retryAfter)
      case c if c >= 500 => ServerError(c, body)
      case 400 => BadRequest(body)
      case 401 => Unauthorized(body)
      case 404 => NotFound(body)
      case c => UnexpectedStatus(c, body)
    }
  }
}
