package domain

import sttp.client3.UriContext
import sttp.model.Uri
import tethys.JsonReader

object UriReader {
  given JsonReader[Uri] = JsonReader.stringReader.map(str => uri"$str")
}
