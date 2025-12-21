package domain.scrapper

import sttp.model.Uri
import tethys.*
import http.protocol.codec.UriCodec.given

final case class LinkUpdate(
    ownerId: Long,
    url: Uri,
    description: String,
) derives JsonWriter
