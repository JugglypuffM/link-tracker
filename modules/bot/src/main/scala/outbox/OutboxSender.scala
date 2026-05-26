package outbox

import canoe.api.TelegramClient
import canoe.methods.messages.SendMessage
import canoe.syntax.methodOps
import cats.effect.*
import cats.syntax.all.*
import config.AppConfig
import fs2.Stream
import kafka.protocol.LinkUpdate
import repository.BotRepository
import tofu.logging.Logging
import tofu.syntax.logging.*

import java.util.UUID
import scala.concurrent.duration.{DurationInt, FiniteDuration}

trait OutboxSender[F[_]] {
  def start: Stream[F, Unit]
}

object OutboxSender {

  private final class Impl(using
      config: AppConfig,
      repo: BotRepository[IO],
      telegram: TelegramClient[IO],
      lm: Logging.Make[IO],
  ) extends OutboxSender[IO] {

    given Logging[IO] = Logging.Make[IO].forService[OutboxSender[IO]]

    def start: Stream[IO, Unit] =
      Stream
        .awakeEvery[IO](config.outbox.pollIntervalSeconds.seconds)
        .evalMap(_ => processBatch)

    private def processBatch: IO[Unit] =
      for {
        batch <- repo.fetchOutboxPending(config.outbox.batchSize)
        _     <- if (batch.nonEmpty) process(batch) else IO.unit
      } yield ()

    private def process(batch: List[LinkUpdate]): IO[Unit] =
      for {
        _          <- info"Sending batch of ${batch.size} notifications"
        successIds <- batch.parTraverseFilter(sendUpdate)
        _ <- if (successIds.nonEmpty)
          repo.markOutboxSent(successIds) *>
            info"Marked ${successIds.size} as SENT"
        else IO.unit
      } yield ()

    private def sendUpdate(update: LinkUpdate): IO[Option[UUID]] =
      SendMessage(update.ownerId, update.description)
        .call
        .as(Some(update.id))
        .handleErrorWith { err =>
          error"Failed to send ${update.id}: $err".as(None)
        }
  }

  def make(using
      AppConfig,
      BotRepository[IO],
      TelegramClient[IO],
      Logging.Make[IO],
  ): OutboxSender[IO] = Impl()
}
