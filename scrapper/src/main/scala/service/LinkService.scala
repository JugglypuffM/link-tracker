package service

import cats.effect.{IO, Ref}
import domain.{AddLinkRequest, LinkResponse}
import repository.LinkRepository
import sttp.model.Uri

trait LinkService[F[_]] {
  def getAllLinks(id: Long): F[List[LinkResponse]]
  def trackLinkByChat(id: Long, link: AddLinkRequest): F[LinkResponse]
  def deleteTrackingForChat(id: Long, link: Uri): F[LinkResponse]
}

object LinkService {
  final private class Impl(repo: LinkRepository[IO], lastIdRef: Ref[IO, Long]) extends LinkService[IO] {

    def getAllLinks(id: Long): IO[List[LinkResponse]] = repo.getLinks(id)

    def trackLinkByChat(id: Long, link: AddLinkRequest): IO[LinkResponse] =
      for {
        lastId <- lastIdRef.get
        linkResponse = LinkResponse(lastId + 1, link.url, link.tags, link.filters)
        _ <- repo.addLink(id, linkResponse)
        _ <- lastIdRef.update(_ + 1)
      } yield linkResponse

    def deleteTrackingForChat(id: Long, link: Uri): IO[LinkResponse] =
      for {
        response <- repo.getLinkByUrl(link)
        _        <- repo.deleteLink(id, link)
      } yield response
  }

  def make(repo: LinkRepository[IO]): IO[LinkService[IO]] =
    for ref <- Ref.of[IO, Long](0)
    yield Impl(repo, ref)
}
