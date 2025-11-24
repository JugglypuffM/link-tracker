package http.protocol

import org.http4s.ember.core.EmberException.ParseError
import sttp.model.Uri
import sttp.tapir.Schema
import http.protocol.codec.Uri.given 
import tethys.*
import tethys.jackson.*

import java.time.ZonedDateTime

case class LinkResponse(
    id: Long,
    url: Uri,
    tags: List[String],
    filters: List[String],
) derives Schema, JsonReader, JsonWriter
