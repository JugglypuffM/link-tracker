package http.protocol.codec

import sttp.client3.UriContext
import sttp.model.Uri
import tethys.{JsonReader, JsonWriter}

object UriCodec {
  given JsonReader[Uri] = JsonReader.stringReader.map(str => uri"$str")
  given JsonWriter[Uri] = JsonWriter.stringWriter.contramap(_.toString)
}
