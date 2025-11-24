package http.controller.endpoints

import http.protocol.ApiErrorResponse
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.json.tethysjson.jsonBody

object TgChatEndpoints {
  val registerEndpoint: Endpoint[Unit, Long, ApiErrorResponse, Unit, Any] =
    endpoint.post
      .summary("Зарегистрировать чат")
      .in("tg-chat" / path[Long]("id"))
      .out(statusCode(StatusCode.Ok).description("Чат успешно зарегистрирован"))
      .errorOut(statusCode(StatusCode.BadRequest)
        .description("Некорректные параметры запроса")
        .and(jsonBody[ApiErrorResponse]))

  val deleteEndpoint: Endpoint[Unit, Long, (StatusCode, ApiErrorResponse), Unit, Any] =
    endpoint.delete
      .summary("Удалить чат")
      .in("tg-chat" / path[Long]("id"))
      .out(statusCode(StatusCode.Ok).description("Чат успешно удален"))
      .errorOut(statusCode
        .description(StatusCode.BadRequest, "Некорректные параметры запроса")
        .description(StatusCode.NotFound, "Чат не существует")
        .and(jsonBody[ApiErrorResponse]))
}
