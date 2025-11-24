package repository

import cats.effect.{IO, Ref}
import domain.link.{Link, LinkInfo, Settings}
import repository.error.{LinkNotFound, SaveError}
import sttp.model.Uri

import scala.util.control.NoStackTrace

trait LinkRepository[F[_]] {
  def save(chatId: Long, url: Uri, settings: Settings): F[LinkInfo]

  def delete(chatId: Long, url: Uri): F[LinkInfo]

  def allLinks: F[List[Link]]
  
  def allInfosFor(url: Uri): F[List[LinkInfo]]

  def get(chatId: Long, url: Uri): F[LinkInfo]
}

object LinkRepository {
  final private class InMemory(using repo: Ref[IO, Set[LinkInfo]], lastId: Ref[IO, Long]) extends LinkRepository[IO] {
    override def save(chatId: Long, url: Uri, settings: Settings): IO[LinkInfo] =
      for {
        r <- repo.get
        _ <- if (r.exists(info => (chatId == info.chatId) && (url == info.link.url))) IO.raiseError(SaveError()) else IO(())
        i <- lastId.get
        id = i + 1
        info <- r.find(url == _.link.url) match
          case Some(LinkInfo(link, _, _, _)) =>
            val info = LinkInfo(link, chatId, settings, None)
            repo.update(r => r + info) >> IO(info)

          case None =>
            for {
              i <- lastId.get
              id = i + 1
              info = LinkInfo(Link(id, url), chatId, settings, None)
              _ <- repo.update(r => r + info)
              _ <- lastId.update(_ + 1)
            } yield info
      } yield info

    override def delete(chatId: Long, url: Uri): IO[LinkInfo] =
      for {
        r <- repo.get
        existingLink <- r.find(info => (chatId == info.chatId) && (url == info.link.url)) match
          case Some(link) => repo.update(_ - link) >> IO(link)
          case None => IO.raiseError(LinkNotFound(url))
      } yield existingLink

    override def allLinks: IO[List[Link]] =
      for r <- repo.get
        yield r.map(_.link).toList.distinct

    override def allInfosFor(url: Uri): IO[List[LinkInfo]] =
      for r <- repo.get
        yield r.collect{case info if info.link.url == url => info}.toList

    override def get(chatId: Long, url: Uri): IO[LinkInfo] =
      for {
        r <- repo.get
        link <- r.find(info => (chatId == info.chatId) && (url == info.link.url)) match
          case Some(v) => IO(v)
          case _ => IO.raiseError(LinkNotFound(url))
      } yield link
  }

  def makeInMemory(using Ref[IO, Set[LinkInfo]]): IO[LinkRepository[IO]] =
    for given Ref[IO, Long] <- Ref.of[IO, Long](0)
      yield InMemory()
}
