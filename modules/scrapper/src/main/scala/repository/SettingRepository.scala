package repository

import cats.effect.{Clock, IO}
import domain.link.Setting
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import repository.error.{LinkNotFound, SaveError}
import sttp.model.Uri

import java.time.Instant

trait SettingRepository[F[_]] {
  def save(chatId: Long, url: Uri): F[Setting]
  def update(id: Long, updatedAt: Instant): F[Unit]
  def delete(chatId: Long, url: Uri): F[Setting]
  def allUniqueSettings: F[List[Setting]]
  def settingsFor(chatId: Long): F[List[Setting]]
}

object SettingRepository {

  private final class DoobieImpl(using transactor: Transactor[IO]) extends SettingRepository[IO] {

    private implicit val settingRead: Read[Setting] =
      Read[(Long, Long, String, Instant)].map {
        case (id, chatId, url, updatedAt) =>
          Setting(id, chatId, Uri.unsafeParse(url), updatedAt)
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
            WHERE id = $id AND (updated_at is NULL OR updated_at < $updatedAt)
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

    override def allUniqueSettings: IO[List[Setting]] =
      sql"""
              SELECT DISTINCT ON (link) id, owner_id, link, updated_at
              FROM settings
            """.query[Setting].to[List].transact(transactor)

    override def settingsFor(chatId: Long): IO[List[Setting]] =
      sql"""
              SELECT id, ownerId, link, updatedAt
              FROM settings
              WHERE ownerId = $chatId
            """.query[Setting].to[List].transact(transactor)
  }

  def makeDoobie(using transactor: Transactor[IO]): SettingRepository[IO] = new DoobieImpl
}
