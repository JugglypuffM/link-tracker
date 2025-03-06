package controller

import cats.effect.IO
import controller.endpoints.TgChatEndpoints
import domain.ApiErrorResponse
import repository.ChatRepository.ChatNotFoundException
import service.ChatService
import sttp.model.StatusCode
import sttp.tapir.server.ServerEndpoint

class TgChatController(
    chatService: ChatService[IO]
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
        case Left(err: ChatNotFoundException) =>
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
