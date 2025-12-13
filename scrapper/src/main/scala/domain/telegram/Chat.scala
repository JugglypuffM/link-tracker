package domain.telegram

import sttp.model.Uri

case class Chat(
    id: Long,
    links: Set[Uri],
)
