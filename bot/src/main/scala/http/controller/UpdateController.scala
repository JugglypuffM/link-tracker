package http.controller

import cats.effect.IO
import sttp.tapir.*
import sttp.tapir.server.ServerEndpoint
import tofu.logging.Logging
import tofu.syntax.logging.LoggingInterpolator
import cats.syntax.all.*
import http.protocol.LinkUpdate
import service.UpdateService

class UpdateController(service: UpdateService)(using lm: Logging.Make[IO]) {
  given Logging[IO] = Logging.Make[IO].forService[UpdateController]

  private val update: ServerEndpoint[Any, IO] = UpdateEndpoints.updateEndpoint.serverLogic {
    linkUpdate =>
      for {
        _ <- infoWith"Received update" ("link" -> linkUpdate.url.toString, "description" -> linkUpdate.description)
        _ <- service.sendUpdates(linkUpdate)
      } yield Right(())
  }

  val endpoints: List[ServerEndpoint[Any, IO]] = List(update).map(_.withTag("Links"))
}
