package http.protocol

import sttp.tapir.Schema
import tethys.*
import tethys.jackson.*

case class ApiErrorResponse(
    description: String,
    code: String,
    exceptionName: String,
    exceptionMessage: String,
    stacktrace: List[String]
) derives Schema, JsonReader, JsonWriter

object ApiErrorResponse {
  def fromException(description: String, code: String, err: Throwable): ApiErrorResponse =
    ApiErrorResponse(
      description,
      code,
      err.getClass.getSimpleName,
      err.getMessage,
      err.getStackTrace.toList.map(_.toString)
    )
}
