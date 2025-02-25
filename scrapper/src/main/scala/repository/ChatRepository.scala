package repository

import cats.effect.{IO, Ref}
import domain.LinkResponse

trait ChatRepository[F[_]] {
  def create(id: Long): F[Unit]
  def delete(id: Long): F[Unit]
}

object ChatRepository {
  class ChatNotFoundException extends Exception

  final private class ImMemory(
      repo: Ref[IO, (Map[Long, Set[LinkResponse]], Map[Long, Set[Long]])]
  ) extends ChatRepository[IO] {
    override def create(id: Long): IO[Unit] =
      repo.update(data => (data._1 + (id -> Set.empty), data._2))

    override def delete(id: Long): IO[Unit] =
      for {
        data <- repo.get
        elem = data._1.find(id == _._1)
        _ <- elem match
          case Some((id, _)) => repo.update(data => (data._1 - id, data._2))
          case None          => IO.raiseError(ChatNotFoundException())
      } yield ()
  }

  def make(repo: Ref[IO, (Map[Long, Set[LinkResponse]], Map[Long, Set[Long]])]): ChatRepository[IO] = ImMemory(repo)
}
