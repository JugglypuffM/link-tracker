package repository

import cats.effect.{IO, Ref}

trait ChatRepository[F[_]] {
  def create(id: Long): F[Unit]
  def delete(id: Long): F[Unit]
}

object ChatRepository {
  class ChatNotFoundException extends Exception

  final private class ImMemory(
      repo: Ref[IO, InMemoryRepo]
  ) extends ChatRepository[IO] {
    override def create(id: Long): IO[Unit] =
      repo.update(r => InMemoryRepo(r.links, r.chatToLinks + (id -> Set.empty), r.linkToChats))

    override def delete(id: Long): IO[Unit] =
      repo.get.flatMap { r =>
        r.chatToLinks.find(id == _._1) match
          case Some((id, _)) => repo.update(data =>
              InMemoryRepo(
                data.links,
                data.chatToLinks - id,
                data.linkToChats.map {
                  case (k, v) => k -> (v - id)
                }
              )
            )
          case None => IO.raiseError(ChatNotFoundException())
      }

  }

  def makeInMemory(repo: Ref[IO, InMemoryRepo]): ChatRepository[IO] = ImMemory(repo)
}
