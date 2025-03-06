package domain

import domain.UriReader.given
import org.http4s.ember.core.EmberException.ParseError
import sttp.model.Uri
import sttp.tapir.Schema
import tethys.readers.FieldName
import tethys.readers.tokens.TokenIterator
import tethys.{JsonReader, JsonWriter}

case class LinkResponse(
    id: Long,
    url: Uri,
    tags: List[String],
    filters: List[String]
) derives Schema

object LinkResponse {
  given JsonReader[LinkResponse] = JsonReader.builder
    .addField[Long]("id")
    .addField[Uri]("uri")
    .addField[List[String]]("tags")
    .addField[List[String]]("filters")
    .buildReader(LinkResponse.apply)

  given JsonWriter[LinkResponse] = JsonWriter.obj[LinkResponse]
    .addField("id")(_.id)
    .addField("uri")(_.url.toString)
    .addField("tags")(_.tags)
    .addField("filters")(_.filters)
}
