package service

import cats.syntax.all.toFoldableOps
import canoe.api.TelegramClient
import canoe.methods.messages.SendMessage
import canoe.syntax.*
import cats.effect.IO
import http.protocol.LinkUpdate
import sttp.model.Uri
import tofu.logging.Logging
import tofu.syntax.location.logging.LoggingInterpolator

class UpdateService(using c: TelegramClient[IO], lm: Logging.Make[IO]) {
  given Logging[IO] = Logging.Make[IO].forService[UpdateService]
  private def sendUpdate(id: Long, url: Uri, updateMsg: String): IO[Unit] =
    for {
      _ <- SendMessage(id, updateMsg).call
      _ <- infoWith"Update sent" ("chat-id" -> id, "link" -> url.toString, "description" -> updateMsg)
    } yield ()

  def sendUpdates(linkUpdate: LinkUpdate): IO[Unit] =
    linkUpdate.tgChatIds.traverse_(sendUpdate(_, linkUpdate.url, linkUpdate.toMsg))
}
