package repository

import cats.effect.{IO, Ref}
import domain.LinkResponse
import repository.ChatRepository.ChatNotFoundException
import sttp.model.Uri

trait LinkRepository[F[_]] {
  def getLinkByUrl(url: Uri): F[LinkResponse]
  def getLinks(id: Long): F[List[LinkResponse]]
  def addLink(id: Long, link: LinkResponse): F[Unit]
  def deleteLink(id: Long, link: Uri): F[Unit]
}

object LinkRepository {
  class LinkNotFoundException extends Exception

  final private case class InMemory(
      repo: Ref[IO, InMemoryRepo]
  ) extends LinkRepository[IO] {
    def getLinkByUrl(url: Uri): IO[LinkResponse] =
      repo.get.map(r =>
        if (!r.links.contains(url)) throw LinkNotFoundException()

        r.links(url)
      )

    def getLinks(id: Long): IO[List[LinkResponse]] =
      repo.get.map(r =>
        if (!r.chatToLinks.contains(id)) throw ChatNotFoundException()

        r.chatToLinks(id).toList.map(r.links(_))
      )

    def addLink(id: Long, link: LinkResponse): IO[Unit] =
      repo.update(r =>
        if (!r.chatToLinks.contains(id)) throw ChatNotFoundException()

        InMemoryRepo(
          r.links + (link.url       -> link),
          r.chatToLinks + (id       -> Set(link.url)),
          r.linkToChats + (link.url -> Set(id))
        )
      )

    def deleteLink(id: Long, link: Uri): IO[Unit] =
      repo.update(r => {
        val chatLinks = r.chatToLinks.getOrElse(id, throw ChatNotFoundException())
        if (!chatLinks.contains(link)) throw LinkNotFoundException()

        InMemoryRepo(
          if ((r.chatToLinks(id) - link).isEmpty) r.links - link
          else r.links,
          r.chatToLinks.updated(id, r.chatToLinks(id) - link),
          if ((r.linkToChats(link) - id).isEmpty) r.linkToChats - link
          else r.linkToChats.updated(link, r.linkToChats(link) - id)
        )
      })

  }

  def makeInMemory(repo: Ref[IO, InMemoryRepo]): LinkRepository[IO] = InMemory(repo)
}
