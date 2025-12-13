package domain.link

import http.protocol.LinkResponse
import sttp.model.Uri

import java.time.ZonedDateTime

case class LinkInfo(
    link: Link,
    chatId: Long,
    lastUpdatedAt: Option[ZonedDateTime]
) {
  def toLinkResponse: LinkResponse =
    LinkResponse(
      id = link.id,
      url = link.url,
    )
}
