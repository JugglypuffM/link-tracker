package service

import cats.effect.IO
import repository.ChatRepository

trait ChatService[F[_]] {
  def register(chatId: Long): F[Unit]
  def delete(chatId: Long): F[Unit]
}

object ChatService{
  final private class Impl(repo: ChatRepository[IO]) extends ChatService[IO]{
    override def register(chatId: Long): IO[Unit] = repo.create(chatId)

    override def delete(chatId: Long): IO[Unit] = repo.delete(chatId)
  }
  
  def make(repository: ChatRepository[IO]): ChatService[IO] = Impl(repository)
}
