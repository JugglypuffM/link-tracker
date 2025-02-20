package controller.endpoints

import domain.*
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.json.tethysjson.jsonBody

object LinksEndpoints {
  val getEndpoint: Endpoint[Unit, Long, ApiErrorResponse, LinkListResponse, Any] =
    endpoint.get
      .summary("Получить все отслеживаемые ссылки")
      .in("links")
      .in(header[Long]("Tg-Chat-Id"))
      .out(statusCode(StatusCode.Ok).and(jsonBody[LinkListResponse]))
      .errorOut(statusCode(StatusCode.BadRequest).and(jsonBody[ApiErrorResponse]))

  val postEndpoint: Endpoint[Unit, (Long, AddLinkRequest), ApiErrorResponse, LinkResponse, Any] =
    endpoint.post
      .summary("Добавить отслеживаемые ссылки")
      .in("links")
      .in(header[Long]("Tg-Chat-Id"))
      .in(jsonBody[AddLinkRequest])
      .out(statusCode(StatusCode.Ok).and(jsonBody[LinkResponse]))
      .errorOut(statusCode(StatusCode.BadRequest).and(jsonBody[ApiErrorResponse]))

  val deleteEndpoint
      : Endpoint[Unit, (Long, RemoveLinkRequest), (ApiErrorResponse, ApiErrorResponse), LinkResponse, Any] =
    endpoint.delete
      .summary("Убрать отслеживание ссылки")
      .in("links")
      .in(header[Long]("Tg-Chat-Id"))
      .in(jsonBody[RemoveLinkRequest])
      .out(statusCode(StatusCode.Ok).and(jsonBody[LinkResponse]))
      .errorOut(statusCode(StatusCode.BadRequest).and(jsonBody[ApiErrorResponse]))
      .errorOut(statusCode(StatusCode.NotFound).and(jsonBody[ApiErrorResponse]))
}
