package service

import cats.effect.IO
import cats.implicits.toTraverseOps
import http.protocol.{AddLinkRequest, LinkResponse, RemoveLinkRequest}
import repository.ScrapperRepository

trait LinkService[F[_]] {
  def getLinksForChat(chatId: Long): F[List[LinkResponse]]
  def trackLinkByChat(chatId: Long, request: AddLinkRequest): F[LinkResponse]
  def deleteTrackingForChat(chatId: Long, request: RemoveLinkRequest): F[LinkResponse]
}

object LinkService {
  final private class Impl(using
                           settingsRepo: ScrapperRepository[IO],
  ) extends LinkService[IO] {
    override def getLinksForChat(chatId: Long): IO[List[LinkResponse]] =
      for {
        settings <- settingsRepo.settingsFor(chatId)
        responses = settings.map(setting => LinkResponse(setting.id, setting.link))
      } yield responses

    override def trackLinkByChat(chatId: Long, request: AddLinkRequest): IO[LinkResponse] =
      for {
        link <- settingsRepo.save(chatId, request.url)
      } yield link.toLinkResponse

    override def deleteTrackingForChat(chatId: Long, request: RemoveLinkRequest): IO[LinkResponse] =
      for {
        link <- settingsRepo.delete(chatId, request.link)
      } yield link.toLinkResponse
  }

  def make(using ScrapperRepository[IO]): LinkService[IO] = Impl()
}
