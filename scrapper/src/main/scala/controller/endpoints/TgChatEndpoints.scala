package controller.endpoints

import domain.ApiErrorResponse
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.json.tethysjson.jsonBody

object TgChatEndpoints {
  val registerEndpoint: Endpoint[Unit, Long, ApiErrorResponse, Unit, Any] =
    endpoint.post
    .summary("Зарегистрировать чат")
    .in("tg-chat" / path[Long]("id"))
    .out(statusCode(StatusCode.Ok))
    .errorOut(statusCode(StatusCode.BadRequest).and(jsonBody[ApiErrorResponse]))

  val deleteEndpoint: Endpoint[Unit, Long, ApiErrorResponse, Unit, Any] =
    endpoint.delete
      .summary("Удалить чат")
      .in("tg-chat" / path[Long]("id"))
      .out(statusCode(StatusCode.Ok))
      .errorOut(statusCode(StatusCode.BadRequest).and(jsonBody[ApiErrorResponse]))
}
