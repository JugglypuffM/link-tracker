package http.protocol

import sttp.model.Uri
import sttp.tapir.Schema
import tethys.{JsonReader, JsonWriter}

case class LinkListResponse(
    links: List[LinkResponse],
    size: Int
) derives Schema

object LinkListResponse {
  given JsonReader[LinkListResponse] = JsonReader.builder
    .addField[List[LinkResponse]]("links")
    .addField[Int]("size")
    .buildReader(LinkListResponse.apply)

  given JsonWriter[LinkListResponse] = JsonWriter.obj[LinkListResponse]
    .addField("links")(_.links)
    .addField("size")(_.size)
}
