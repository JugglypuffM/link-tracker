package http.controller.endpoints

import domain.*
import http.protocol.{AddLinkRequest, ApiErrorResponse, LinkListResponse, LinkResponse, RemoveLinkRequest}
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.json.tethysjson.jsonBody

object LinksEndpoints {
  val getEndpoint: Endpoint[Unit, Long, ApiErrorResponse, LinkListResponse, Any] =
    endpoint.get
      .summary("Получить все отслеживаемые ссылки")
      .in("links")
      .in(header[Long]("Tg-Chat-Id"))
      .out(statusCode(StatusCode.Ok).description("Ссылки успешно получены").and(jsonBody[LinkListResponse]))
      .errorOut(statusCode(StatusCode.BadRequest)
        .description("Некорректные параметры запроса")
        .and(jsonBody[ApiErrorResponse]))

  val postEndpoint: Endpoint[Unit, (Long, AddLinkRequest), ApiErrorResponse, LinkResponse, Any] =
    endpoint.post
      .summary("Добавить отслеживаемые ссылки")
      .in("links")
      .in(header[Long]("Tg-Chat-Id"))
      .in(jsonBody[AddLinkRequest])
      .out(statusCode(StatusCode.Ok).description("Ссылка успешно добавлена").and(jsonBody[LinkResponse]))
      .errorOut(statusCode(StatusCode.BadRequest)
        .description("Некорректные параметры запроса")
        .and(jsonBody[ApiErrorResponse]))

  val deleteEndpoint: Endpoint[Unit, (Long, RemoveLinkRequest), (StatusCode, ApiErrorResponse), LinkResponse, Any] =
    endpoint.delete
      .summary("Убрать отслеживание ссылки")
      .in("links")
      .in(header[Long]("Tg-Chat-Id"))
      .in(jsonBody[RemoveLinkRequest])
      .out(statusCode(StatusCode.Ok).description("Ссылка успешно убрана").and(jsonBody[LinkResponse]))
      .errorOut(statusCode
        .description(StatusCode.BadRequest, "Некорректные параметры запроса")
        .description(StatusCode.NotFound, "Ссылка не найдена")
        .and(jsonBody[ApiErrorResponse]))
}
