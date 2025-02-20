package controller

import cats.effect.IO
import controller.endpoints.LinksEndpoints
import domain.{LinkListResponse, LinkResponse}
import sttp.model.Uri
import sttp.tapir.server.ServerEndpoint

class LinksController extends Controller[IO] {
  private val get: ServerEndpoint[Any, IO] =
    LinksEndpoints.getEndpoint.serverLogic(id => IO(Right(LinkListResponse(List(), 0))))

  private val post: ServerEndpoint[Any, IO] =
    LinksEndpoints.postEndpoint.serverLogic((id, request) =>
      IO(Right(LinkResponse(0, Uri.parse("/update").toOption.get, List(), List())))
    )

  private val delete: ServerEndpoint[Any, IO] =
    LinksEndpoints.deleteEndpoint.serverLogic((id, request) =>
      IO(Right(LinkResponse(0, Uri.parse("/update").toOption.get, List(), List())))
    )

  override val endpoints: List[ServerEndpoint[Any, IO]] = List(get, post, delete).map(_.withTag("Links"))
}
