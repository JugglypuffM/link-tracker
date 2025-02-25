package controller

import cats.effect.IO
import controller.endpoints.TgChatEndpoints
import domain.ApiErrorResponse
import repository.ChatRepository.ChatNotFoundException
import service.ChatService
import sttp.tapir.server.ServerEndpoint

class TgChatController(
    chatService: ChatService[IO]
) extends Controller[IO] {
  private val register: ServerEndpoint[Any, IO] =
    TgChatEndpoints.registerEndpoint.serverLogic(id =>
      chatService.register(id).attempt.map {
        case Left(err) =>
          Left(ApiErrorResponse(
            "Unexpected error",
            "400",
            err.getClass.getSimpleName,
            err.getMessage,
            err.getStackTrace.toList.map(_.toString)
          ))
        case Right(v) => Right(v)
      }
    )

  private val delete: ServerEndpoint[Any, IO] =
    TgChatEndpoints.deleteEndpoint.serverLogic(id =>
      chatService.delete(id).attempt.map {
        case Left(err: ChatNotFoundException) =>
          Left(ApiErrorResponse(
            "Chat with provided id was not found",
            "404",
            err.getClass.getSimpleName,
            err.getMessage,
            err.getStackTrace.toList.map(_.toString)
          ))

        case Left(err) =>
          Left(ApiErrorResponse(
            "Unexpected error",
            "400",
            err.getClass.getSimpleName,
            err.getMessage,
            err.getStackTrace.toList.map(_.toString)
          ))

        case Right(v) => Right(v)
      }
    )

  override val endpoints: List[ServerEndpoint[Any, IO]] = List(register, delete).map(_.withTag("TgChats"))
}
