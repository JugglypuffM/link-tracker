package controller

import cats.effect.IO
import sttp.tapir.*
import sttp.tapir.server.ServerEndpoint

class UpdateController {
  private val update: ServerEndpoint[Any, IO] = UpdateEndpoints.updateEndpoint.serverLogic{
    linkUpdate => IO(Right(()))
  }

  val endpoints: List[ServerEndpoint[Any, IO]] = List(update)
}

