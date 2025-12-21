package kafka.protocol

import http.protocol.codec.UriCodec.given
import sttp.model.Uri
import tethys.*

final case class LinkUpdate(
    ownerId: Long,
    url: Uri,
    description: String,
) derives JsonReader {
  def toMessage: String = s"Новое обновление по ссылке $url: $description"
}
