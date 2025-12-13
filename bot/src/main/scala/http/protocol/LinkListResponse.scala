package http.protocol

import tethys.*

case class LinkListResponse(
    links: List[LinkResponse]
) derives JsonReader
