package domain

import sttp.model.Uri
import tethys.JsonReader

object UriReader {
  given JsonReader[Uri] = JsonReader.stringReader.map(Uri.apply)
}
