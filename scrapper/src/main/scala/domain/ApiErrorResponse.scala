package domain

import sttp.tapir.Schema
import tethys.{JsonReader, JsonWriter}

case class ApiErrorResponse(
    description: String,
    code: String,
    exceptionName: String,
    exceptionMessage: String,
    stacktrace: List[String]
) derives Schema

object ApiErrorResponse {
  given JsonReader[ApiErrorResponse] = JsonReader.builder
    .addField[String]("description")
    .addField[String]("code")
    .addField[String]("exceptionName")
    .addField[String]("exceptionMessage")
    .addField[List[String]]("stacktrace")
    .buildReader(ApiErrorResponse.apply)

  given JsonWriter[ApiErrorResponse] = JsonWriter.obj[ApiErrorResponse]
    .addField("description")(_.description)
    .addField("code")(_.code)
    .addField("exceptionName")(_.exceptionName)
    .addField("exceptionMessage")(_.exceptionMessage)
    .addField("stacktrace")(_.stacktrace)

  def fromException(description: String, code: String, err: Throwable): ApiErrorResponse =
    ApiErrorResponse(
      description,
      code,
      err.getClass.getSimpleName,
      err.getMessage,
      err.getStackTrace.toList.map(_.toString)
    )
}
