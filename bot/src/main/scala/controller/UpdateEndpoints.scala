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
    .out(statusCode(StatusCode.Ok))
    .errorOut(statusCode(StatusCode.BadRequest).and(jsonBody[ApiErrorResponse]))

}
