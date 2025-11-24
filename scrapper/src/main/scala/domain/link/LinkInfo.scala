package domain.link

import http.protocol.LinkResponse
import sttp.model.Uri

import java.time.ZonedDateTime

case class LinkInfo(
                 link: Link,
                 chatId: Long,
                 settings: Settings,
                 lastUpdatedAt: Option[ZonedDateTime]
) {
  def toLinkResponse: LinkResponse =
    LinkResponse(
      id = link.id,
      url = link.url,
      tags = settings.tags,
      filters = settings.filters
    )
}
