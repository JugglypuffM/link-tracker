package controller

import domain.{ApiErrorResponse, LinkUpdate}
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.json.tethysjson.jsonBody

object UpdateEndpoints {
  val updateEndpoint: Endpoint[Unit, LinkUpdate, ApiErrorResponse, Unit, Any] = endpoint.post
    .summary("Отправить обновление")
    .in("updates")
    .in(jsonBody[LinkUpdate])
    .out(statusCode(StatusCode.Ok).description("Обновление обработано"))
    .errorOut(statusCode(StatusCode.BadRequest).description("Некорректные параметры запроса").and(
      jsonBody[ApiErrorResponse]
    ))

}
