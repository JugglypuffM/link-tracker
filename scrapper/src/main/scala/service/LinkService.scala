package service

import cats.effect.IO
import cats.implicits.toTraverseOps
import http.protocol.{AddLinkRequest, LinkResponse, RemoveLinkRequest}
import repository.{ChatRepository, LinkRepository}

trait LinkService[F[_]] {
  def getLinksForChat(chatId: Long): F[List[LinkResponse]]
  def trackLinkByChat(chatId: Long, request: AddLinkRequest): F[LinkResponse]
  def deleteTrackingForChat(chatId: Long, request: RemoveLinkRequest): F[LinkResponse]
}

object LinkService {
  final private class Impl(using
      linkRepo: LinkRepository[IO],
      chatRepo: ChatRepository[IO],
  ) extends LinkService[IO] {
    override def getLinksForChat(chatId: Long): IO[List[LinkResponse]] =
      for {
        urls  <- chatRepo.allLinksFor(chatId)
        links <- urls.traverse(url => linkRepo.get(chatId, url))
        responses = links.map(info => LinkResponse(info.link.id, info.link.url))
      } yield responses

    override def trackLinkByChat(chatId: Long, request: AddLinkRequest): IO[LinkResponse] =
      for {
        link <- linkRepo.save(chatId, request.url)
        _    <- chatRepo.addLink(chatId, request.url)
      } yield link.toLinkResponse

    override def deleteTrackingForChat(chatId: Long, request: RemoveLinkRequest): IO[LinkResponse] =
      for {
        link <- linkRepo.delete(chatId, request.link)
        _    <- chatRepo.removeLink(chatId, request.link)
      } yield link.toLinkResponse
  }

  def make(using LinkRepository[IO], ChatRepository[IO]): LinkService[IO] = Impl()
}
