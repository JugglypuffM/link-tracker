package http.controller

import cats.effect.IO
import http.controller.endpoints.TgChatEndpoints
import http.protocol.ApiErrorResponse
import repository.error.ChatNotFound
import service.ChatService
import sttp.model.StatusCode
import sttp.tapir.server.ServerEndpoint

class TgChatController(
    using chatService: ChatService[IO]
) extends Controller[IO] {
  private val register: ServerEndpoint[Any, IO] =
    TgChatEndpoints.registerEndpoint.serverLogic(id =>
      chatService.register(id).attempt.map {
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
    TgChatEndpoints.deleteEndpoint.serverLogic(id =>
      chatService.delete(id).attempt.map {
        case Left(err: ChatNotFound) =>
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

  override val endpoints: List[ServerEndpoint[Any, IO]] = List(register, delete).map(_.withTag("TgChats"))
}
