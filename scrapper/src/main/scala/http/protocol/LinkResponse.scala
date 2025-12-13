package http.protocol

import org.http4s.ember.core.EmberException.ParseError
import sttp.model.Uri
import sttp.tapir.Schema
import http.protocol.codec.UriCodec.given
import tethys.*
import tethys.jackson.*

import java.time.ZonedDateTime

case class LinkResponse(
    id: Long,
    url: Uri,
) derives Schema, JsonReader, JsonWriter
