package http.controller

import cats.effect.IO
import http.controller.endpoints.LinksEndpoints
import http.protocol.{ApiErrorResponse, LinkListResponse}
import repository.error.LinkNotFound
import service.LinkService
import sttp.model.StatusCode
import sttp.tapir.server.ServerEndpoint

class LinksController(using linkService: LinkService[IO]) extends Controller[IO] {
  private val get: ServerEndpoint[Any, IO] =
    LinksEndpoints.getEndpoint.serverLogic(id =>
      linkService.getLinksForChat(id).attempt.map {
        case Left(err) =>
          Left(ApiErrorResponse.fromException(
            "Invalid parameters",
            "BAD_REQUEST",
            err
          ))
        case Right(v) => Right(LinkListResponse(v, v.length))
      }
    )

  private val post: ServerEndpoint[Any, IO] =
    LinksEndpoints.postEndpoint.serverLogic((id, request) =>
      linkService.trackLinkByChat(id, request).attempt.map {
        case Left(err) =>
          Left(ApiErrorResponse.fromException(
            "Invalid parameters",
            "BAD_REQUEST",
            err
          ))
        case Right(v) => Right(v)
      }
    )

  private val delete: ServerEndpoint[Any, IO] =
    LinksEndpoints.deleteEndpoint.serverLogic((id, request) =>
      linkService.deleteTrackingForChat(id, request).attempt.map {
        case Left(err: LinkNotFound) =>
          Left(
            StatusCode.NotFound,
            ApiErrorResponse.fromException(
              "Chat with provided id was not found",
              "NOT_FOUND",
              err
            )
          )

        case Left(err) =>
          Left(
            StatusCode.BadRequest,
            ApiErrorResponse.fromException(
              "Invalid parameters",
              "BAD_REQUEST",
              err
            )
          )

        case Right(v) => Right(v)
      }
    )

  override val endpoints: List[ServerEndpoint[Any, IO]] = List(get, post, delete).map(_.withTag("Links"))
}
