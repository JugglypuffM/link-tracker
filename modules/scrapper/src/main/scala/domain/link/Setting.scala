package domain.link

import http.protocol.LinkResponse
import sttp.model.Uri

import java.time.Instant

case class Setting(id: Long, ownerId: Long, link: Uri, lastUpdatedAt: Instant) {
  def toLinkResponse: LinkResponse =
    LinkResponse(
      id = id,
      url = link,
    )
}
