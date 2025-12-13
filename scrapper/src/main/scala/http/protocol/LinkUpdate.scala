package http.protocol

import http.protocol.codec.UriCodec.given
import sttp.model.Uri
import sttp.tapir.Schema
import tethys.*
import tethys.jackson.*

final case class LinkUpdate(
    id: Long,
    url: Uri,
    description: String,
    tgChatIds: List[Long]
) derives Schema, JsonReader, JsonWriter
