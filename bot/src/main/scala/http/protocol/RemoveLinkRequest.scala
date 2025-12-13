package http.protocol

import http.protocol.codec.UriCodec.given
import sttp.model.Uri
import tethys.*

case class RemoveLinkRequest(
    link: Uri
) derives JsonWriter
