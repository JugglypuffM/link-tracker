package controller

import cats.effect.IO
import controller.endpoints.TgChatEndpoints
import sttp.tapir.server.ServerEndpoint

class TgChatController extends Controller[IO] {
  private val register: ServerEndpoint[Any, IO] =
    TgChatEndpoints.registerEndpoint.serverLogic(id => IO(Right(())))
    
  private val delete: ServerEndpoint[Any, IO] =
    TgChatEndpoints.deleteEndpoint.serverLogic(id => IO(Right(())))

  override val endpoints: List[ServerEndpoint[Any, IO]] = List(register, delete).map(_.withTag("TgChats"))
}
