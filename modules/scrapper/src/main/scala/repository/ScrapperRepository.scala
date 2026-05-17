package repository

import cats.data.NonEmptyList
import cats.syntax.all.toFoldableOps
import cats.effect.{Clock, IO}
import domain.link.Setting
import domain.scrapper.LinkUpdate
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import repository.error.{LinkNotFound, SaveError}
import sttp.model.Uri

import java.time.Instant
import java.util.UUID

trait ScrapperRepository[F[_]] {
  def save(chatId: Long, url: Uri): F[Setting]
  def update(id: Long, updatedAt: Instant): F[Unit]
  def delete(chatId: Long, url: Uri): F[Setting]
  def settingsFor(chatId: Long): F[List[Setting]]
  def getBatch(batchSize: Int, linkProcessTimeMilliseconds: Int, checkPeriodSeconds: Int): IO[List[Setting]]
  def saveUpdate(url: Uri, newTime: Instant, description: String): IO[Unit]
  def fetchOutboxPending(limit: Int): IO[List[LinkUpdate]]
  def markOutboxSent(ids: List[UUID]): IO[Unit]
}

object ScrapperRepository {

  private final class DoobieImpl(using transactor: Transactor[IO]) extends ScrapperRepository[IO] {

    private implicit val settingRead: Read[Setting] =
      Read[(Long, Long, String, Instant)].map {
        case (id, chatId, url, updatedAt) =>
          Setting(id, chatId, Uri.unsafeParse(url), updatedAt)
      }

    private implicit val linkUpdateRead: Read[LinkUpdate] =
      Read[(UUID, Long, String, String, String)].map {
        case (id, ownerId, url, description, status) =>
          LinkUpdate(id, ownerId, Uri.unsafeParse(url), description, status)
      }

    override def save(chatId: Long, url: Uri): IO[Setting] = {
      val e = for {
        now <- Clock[IO].realTimeInstant
        urlString = url.toString()
        setting <- sql"""
              INSERT INTO settings (owner_id, link, updated_at)
              VALUES ($chatId, $urlString, $now)
              RETURNING id, owner_id, link, updated_at
            """.query[Setting].unique.transact(transactor)
      } yield setting

      e.handleErrorWith(error => IO.raiseError(SaveError()))
    }

    override def update(id: Long, updatedAt: Instant): IO[Unit] =
      sql"""
            UPDATE settings
            SET updated_at = $updatedAt
            WHERE id = $id AND updated_at < $updatedAt
           """.update.run.transact(transactor).as(())

    override def delete(chatId: Long, url: Uri): IO[Setting] = {
      val urlString = url.toString()

      sql"""
              DELETE FROM settings
              WHERE owner_id = $chatId AND link = $urlString
              RETURNING id, owner_id, link, updated_at
            """.query[Setting].option.transact(transactor)
        .flatMap {
          case Some(setting) => IO.pure(setting)
          case None          => IO.raiseError(LinkNotFound(url))
        }
    }

    override def settingsFor(chatId: Long): IO[List[Setting]] =
      sql"""
              SELECT id, ownerId, link, updatedAt
              FROM settings
              WHERE ownerId = $chatId
            """.query[Setting].to[List].transact(transactor)

    override def getBatch(batchSize: Int, linkProcessMilliseconds: Int, checkIntervalSeconds: Int): IO[List[Setting]] = {
      val intervalSeconds = (batchSize * (linkProcessMilliseconds / 1000)) + checkIntervalSeconds

      val action = for {
        candidates <- sql"""
          WITH targets AS (
            SELECT DISTINCT ON (link) id
            FROM settings
            WHERE last_check IS NULL OR last_check < now() - make_interval(secs => $intervalSeconds)
            ORDER BY link, last_check NULLS FIRST
            LIMIT $batchSize
          )
          SELECT s.id, s.owner_id, s.link, s.updated_at, s.last_check
          FROM settings s
          WHERE s.id IN (SELECT id FROM targets)
          FOR UPDATE SKIP LOCKED
        """.query[Setting].to[List]

        ids = candidates.map(_.id)

        _ <- NonEmptyList.fromList(ids) match {
          case Some(nel) =>
            (sql"UPDATE settings SET last_check = now() WHERE " ++
              Fragments.in(fr"id", nel)).update.run
          case None => FC.unit
        }
      } yield candidates

      action.transact(transactor)
    }

    override def saveUpdate(uri: Uri, newTime: Instant, description: String): IO[Unit] = {
      val url = uri.toString
      val action = for {
        _ <-
          sql"""
              UPDATE settings
              SET updated_at = $newTime
              WHERE link = $url. AND updated_at < $newTime
             """.update.run

        owners <- sql"""
                  SELECT owner_id FROM settings WHERE link = $url
                """.query[Long].to[List]

        _ <- owners.traverse_ { ownerId =>
          sql"""
                INSERT INTO outbox (id, owner_id, url, updated_at, description)
                VALUES (${UUID.randomUUID()}, $ownerId, $url, $newTime, $description)
                ON CONFLICT (url, updated_at) DO NOTHING
              """.update.run
        }
      } yield ()

      action.transact(transactor)
    }

    override def fetchOutboxPending(limit: Int): IO[List[LinkUpdate]] = {
      sql"""
        SELECT id, owner_id, url, description, status
        FROM outbox
        WHERE status = 'PENDING'
        ORDER BY id
        LIMIT $limit
        FOR UPDATE SKIP LOCKED
      """.query[LinkUpdate].to[List].transact(transactor)
    }

    override def markOutboxSent(ids: List[UUID]): IO[Unit] = {
      NonEmptyList.fromList(ids) match {
        case Some(nel) =>
          (sql"UPDATE outbox SET status = 'SENT' WHERE " ++ Fragments.in(fr"id", nel))
            .update.run.transact(transactor).void
        case None => IO.unit
      }
    }
  }

  def makeDoobie(using transactor: Transactor[IO]): ScrapperRepository[IO] = new DoobieImpl
}
