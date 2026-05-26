package kafka.protocol

import http.protocol.codec.UriCodec.given
import sttp.model.Uri
import tethys.*

import java.util.UUID

final case class LinkUpdate(
    id: UUID,
    ownerId: Long,
    url: Uri,
    description: String,
    status: String
) derives JsonReader {
  def toMessage: String = s"Новое обновление по ссылке $url: $description"
}
