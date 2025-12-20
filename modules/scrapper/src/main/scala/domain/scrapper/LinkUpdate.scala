package domain.scrapper

import http.protocol.codec.UriCodec.given
import sttp.model.Uri
import sttp.tapir.Schema
import tethys.*
import tethys.jackson.*

final case class LinkUpdate(
    ownerId: Long,
    url: Uri,
    description: String,
)
