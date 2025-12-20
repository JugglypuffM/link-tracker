package http.protocol

import http.protocol.codec.UriCodec.given
import sttp.model.Uri
import sttp.tapir.Schema
import tethys.*

case class AddLinkRequest(
    url: Uri,
) derives Schema, JsonReader, JsonWriter
