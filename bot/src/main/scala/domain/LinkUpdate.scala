package domain

import org.http4s.ember.core.EmberException.ParseError
import sttp.model.Uri
import sttp.tapir.Schema
import tethys.readers.FieldName
import tethys.readers.tokens.TokenIterator
import tethys.{JsonReader, JsonWriter}

final case class LinkUpdate(
    id: Int,
    url: Uri,
    description: String,
    tgChatIds: List[Int]
) derives Schema

object LinkUpdate {
  given JsonReader[LinkUpdate] = JsonReader.builder
    .addField[Int]("id")
    .addField[Uri]("uri")
    .addField[String]("description")
    .addField[List[Int]]("tgChatIds")
    .buildReader(LinkUpdate.apply)

  given JsonWriter[LinkUpdate] = JsonWriter.obj[LinkUpdate]
    .addField("id")(_.id)
    .addField("uri")(_.url.toString)
    .addField("description")(_.description)
    .addField("tgChatIds")(_.tgChatIds)

  given JsonReader[Uri] = new JsonReader[Uri] {
    def read(it: TokenIterator)(implicit fieldName: FieldName): Uri = {
      val uriString = it.string()
      Uri.parse(uriString).fold(
        error => throw ParseError(s"Invalid sttp Uri: $error"),
        uri => uri
      )
    }
  }
}
