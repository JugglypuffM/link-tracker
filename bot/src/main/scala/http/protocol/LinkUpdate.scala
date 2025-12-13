package http.protocol

import sttp.model.Uri

final case class LinkUpdate(
    id: Long,
    url: Uri,
    description: String,
    tgChatIds: List[Long]
) {
  def toMsg: String = s"По ссылке $url пришло обновление: $description"
}
