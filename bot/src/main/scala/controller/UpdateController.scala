package controller

import cats.effect.IO
import domain.LinkUpdate
import sttp.tapir.*
import sttp.tapir.server.ServerEndpoint
import tofu.logging.Logging
import tofu.syntax.logging.LoggingInterpolator
import cats.syntax.all.*

class UpdateController(sendUpdate: (Long, LinkUpdate) => IO[Unit])(using lm: Logging.Make[IO]) {
  given Logging[IO] = Logging.Make[IO].forService[UpdateController]

  private val update: ServerEndpoint[Any, IO] = UpdateEndpoints.updateEndpoint.serverLogic {
    linkUpdate =>
      for {
        _ <- infoWith"Received update" ("link" -> linkUpdate.url.toString, "description" -> linkUpdate.description)
        _ <- linkUpdate.tgChatIds.traverse_(sendUpdate(_, linkUpdate))
      } yield Right(())
  }

  val endpoints: List[ServerEndpoint[Any, IO]] = List(update).map(_.withTag("Links"))
}
