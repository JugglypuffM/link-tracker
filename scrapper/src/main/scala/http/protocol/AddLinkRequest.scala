package http.protocol

import sttp.model.Uri
import sttp.tapir.Schema
import http.protocol.codec.Uri.given
import tethys.*
import tethys.jackson.*

case class AddLinkRequest(
    url: Uri,
    tags: List[String],
    filters: List[String]
) derives Schema, JsonReader, JsonWriter
