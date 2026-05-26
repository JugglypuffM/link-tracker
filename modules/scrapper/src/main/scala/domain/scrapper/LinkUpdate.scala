package domain.scrapper

import sttp.model.Uri
import tethys.*
import http.protocol.codec.UriCodec.given

import java.util.UUID

final case class LinkUpdate(
    id: UUID,
    ownerId: Long,
    url: Uri,
    description: String,
    status: String
) derives JsonWriter
