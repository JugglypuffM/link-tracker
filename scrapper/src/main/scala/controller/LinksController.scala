package controller

import cats.effect.IO
import controller.endpoints.LinksEndpoints
import domain.{ApiErrorResponse, LinkListResponse, LinkResponse}
import repository.LinkRepository.LinkNotFoundException
import service.LinkService
import sttp.model.StatusCode
import sttp.tapir.server.ServerEndpoint

class LinksController(linkService: LinkService[IO]) extends Controller[IO] {
  private val get: ServerEndpoint[Any, IO] =
    LinksEndpoints.getEndpoint.serverLogic(id =>
      linkService.getAllLinks(id).attempt.map {
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
      linkService.deleteTrackingForChat(id, request.link).attempt.map {
        case Left(err: LinkNotFoundException) =>
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
