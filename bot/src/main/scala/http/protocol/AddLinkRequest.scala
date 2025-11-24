package http.protocol

import UriReader.given
import sttp.model.Uri
import sttp.tapir.Schema
import tethys.*

case class AddLinkRequest(
    url: Uri,
    tags: List[String],
    filters: List[String]
) derives Schema

object AddLinkRequest {
  given JsonReader[AddLinkRequest] = JsonReader.builder
    .addField[Uri]("url")
    .addField[List[String]]("tags")
    .addField[List[String]]("filters")
    .buildReader(AddLinkRequest.apply)

  given JsonWriter[AddLinkRequest] = JsonWriter.obj[AddLinkRequest]
    .addField("url")(_.url.toString)
    .addField("tags")(_.tags)
    .addField("filters")(_.filters)
}
