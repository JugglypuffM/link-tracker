package http.protocol

import UriReader.given
import sttp.model.Uri
import sttp.tapir.Schema
import tethys.{JsonReader, JsonWriter}

case class RemoveLinkRequest(
    link: Uri
) derives Schema

object RemoveLinkRequest {
  given JsonReader[RemoveLinkRequest] = JsonReader.builder
    .addField[Uri]("link")
    .buildReader(RemoveLinkRequest.apply)

  given JsonWriter[RemoveLinkRequest] = JsonWriter.obj[RemoveLinkRequest]
    .addField("link")(_.link.toString)
}
