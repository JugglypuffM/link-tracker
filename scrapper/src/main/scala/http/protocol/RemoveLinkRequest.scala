package http.protocol

import sttp.model.Uri
import sttp.tapir.Schema
import http.protocol.codec.Uri.given
import tethys.*
import tethys.jackson.*

case class RemoveLinkRequest(
    link: Uri
) derives Schema, JsonReader, JsonWriter
