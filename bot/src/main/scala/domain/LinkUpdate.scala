package domain

import domain.UriReader.given
import sttp.model.Uri
import sttp.tapir.Schema
import tethys.{JsonReader, JsonWriter}

final case class LinkUpdate(
    id: Long,
    url: Uri,
    description: String,
    tgChatIds: List[Long]
) derives Schema

object LinkUpdate {
  given JsonReader[LinkUpdate] = JsonReader.builder
    .addField[Long]("id")
    .addField[Uri]("url")
    .addField[String]("description")
    .addField[List[Long]]("tgChatIds")
    .buildReader(LinkUpdate.apply)

  given JsonWriter[LinkUpdate] = JsonWriter.obj[LinkUpdate]
    .addField("id")(_.id)
    .addField("url")(_.url.toString)
    .addField("description")(_.description)
    .addField("tgChatIds")(_.tgChatIds)
}
