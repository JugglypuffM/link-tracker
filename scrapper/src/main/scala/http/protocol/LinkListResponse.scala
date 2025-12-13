package http.protocol

import sttp.model.Uri
import sttp.tapir.Schema
import tethys.*
import tethys.jackson.*

case class LinkListResponse(
    links: List[LinkResponse],
    size: Int
) derives Schema, JsonReader, JsonWriter
