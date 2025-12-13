package repository

import cats.effect.{IO, Ref}
import domain.link.LinkInfo
import domain.telegram.Chat
import repository.error.{ChatNotFound, SaveError}
import sttp.model.Uri

trait ChatRepository[F[_]] {
  def save(id: Long): F[Unit]
  def delete(id: Long): F[Unit]
  def addLink(id: Long, url: Uri): F[Unit]
  def removeLink(id: Long, url: Uri): F[Unit]
  def allLinksFor(id: Long): F[List[Uri]]
}

object ChatRepository {
  final private class ImMemory(using repo: Ref[IO, Set[Chat]]) extends ChatRepository[IO] {
    override def save(id: Long): IO[Unit] =
      for {
        r <- repo.get
        _ <- if (r.exists(id == _.id)) IO.raiseError(SaveError()) else IO(())
        _ <- repo.update(r => r + Chat(id, Set.empty))
      } yield ()

    override def delete(id: Long): IO[Unit] =
      for {
        r <- repo.get
        _ <- r.find(id == _.id) match
          case Some(chat) => repo.update(_ - chat)
          case None => IO.raiseError(ChatNotFound(id))
      } yield ()

    override def addLink(id: Long, url: Uri): IO[Unit] =
      for {
        r <- repo.get
        _ <- if (!r.exists(id == _.id)) IO.raiseError(ChatNotFound(id)) else IO(())
        _ <- repo.update(_.map {
          case Chat(foundId, links) if foundId == id => Chat(foundId, links + url)
        })
      } yield ()

    override def removeLink(id: Long, url: Uri): IO[Unit] =
      for {
        r <- repo.get
        _ <- if (!r.exists(id == _.id)) IO.raiseError(ChatNotFound(id)) else IO(())
        _ <- repo.update(_.map {
          case Chat(foundId, links) if foundId == id => Chat(foundId, links - url)
        })
      } yield ()

    override def allLinksFor(id: Long): IO[List[Uri]] =
      for {
        r <- repo.get
        links <- r.find(id == _.id) match
          case Some(chat) => IO(chat.links.toList)
          case None => IO.raiseError(ChatNotFound(id))
      } yield links
  }

    def makeInMemory(using Ref[IO, Set[Chat]]): ChatRepository[IO] = ImMemory()
}
