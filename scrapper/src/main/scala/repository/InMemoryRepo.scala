package repository

import domain.LinkResponse
import sttp.model.Uri

case class InMemoryRepo(
    links: Map[Uri, LinkResponse],
    chatToLinks: Map[Long, Set[Uri]],
    linkToChats: Map[Uri, Set[Long]]
)
