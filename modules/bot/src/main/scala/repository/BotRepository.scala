package repository

import cats.effect.IO
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import kafka.protocol.LinkUpdate
import sttp.model.Uri

import java.util.UUID

trait BotRepository[F[_]] {
  def saveUpdate(eventId: UUID, ownerId: Long, url: Uri, description: String): F[Boolean]
  def fetchOutboxPending(limit: Int): F[List[LinkUpdate]]
  def markOutboxSent(eventIds: List[UUID]): IO[Unit]
}

object BotRepository {
  private final class DoobieImpl(using transactor: Transactor[IO]) extends BotRepository[IO] {
    private implicit val linkUpdateRead: Read[LinkUpdate] =
      Read[(UUID, Long, String, String, String)].map {
        case (id, ownerId, url, description, status) =>
          LinkUpdate(id, ownerId, Uri.unsafeParse(url), description, status)
      }

    override def saveUpdate(id: UUID, ownerId: Long, url: Uri, description: String): IO[Boolean] =
      val urlString = url.toString()
      sql"""
        INSERT INTO outbox (event_id, owner_id, url, message_text)
        VALUES ($id, $ownerId, $urlString, $description)
        ON CONFLICT DO NOTHING
      """.update.run.transact(transactor).map(_ > 0)

    override def fetchOutboxPending(limit: Int): IO[List[LinkUpdate]] =
      sql"""
        SELECT event_id, owner_id, message_text, status
        FROM outbox
        WHERE status = 'PENDING'
        ORDER BY event_id
        LIMIT $limit
        FOR UPDATE SKIP LOCKED
      """.query[LinkUpdate].to[List].transact(transactor)

    override def markOutboxSent(ids: List[UUID]): IO[Unit] = {
      import cats.data.NonEmptyList
      NonEmptyList.fromList(ids) match {
        case Some(nel) =>
          (sql"UPDATE outbox SET status = 'SENT' WHERE " ++
            Fragments.in(fr"id", nel)).update.run.transact(transactor).void
        case None => IO.unit
      }
    }
  }

  def makeDoobie(using Transactor[IO]): BotRepository[IO] = DoobieImpl()
}