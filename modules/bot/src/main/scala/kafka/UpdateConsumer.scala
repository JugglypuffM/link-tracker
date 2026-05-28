package kafka

import canoe.api.TelegramClient
import canoe.methods.messages.SendMessage
import canoe.syntax.methodOps
import cats.effect.{IO, Resource}
import cats.implicits.catsSyntaxApplicativeId
import config.AppConfig
import fs2.Stream
import fs2.kafka.*
import kafka.protocol.LinkUpdate
import metrics.BotMetrics
import repository.BotRepository
import scala.concurrent.duration.DurationInt
import tethys.*
import tethys.jackson.*
import tofu.logging.Logging
import tofu.syntax.location.logging.LoggingInterpolator

import java.util.UUID

trait UpdateConsumer[F[_]] {
  def listenUpdates: Stream[F, Unit]
}

object UpdateConsumer {
  private final case class Impl(
      consumer: KafkaConsumer[IO, UUID, LinkUpdate]
  )(using repo: BotRepository[IO], config: AppConfig, metrics: BotMetrics[IO], lm: Logging.Make[IO]) extends UpdateConsumer[IO] {
    given Logging[IO] = Logging.Make[IO].forService[UpdateConsumer[IO]]

    override def listenUpdates: Stream[IO, Unit] = {
      consumer
        .stream
        .evalMap { committable =>
          for {
            startTime <- IO.realTime
            update <- committable.record.value.pure[IO]
            _ <- info"Processing update on link ${update.url.toString} for ${update.ownerId}: ${update.description}"
            _ <- repo.saveUpdate(update.id, update.ownerId, update.url, update.description)
            _ <- info"Saved to outbox update on link ${update.url.toString} for ${update.ownerId}: ${update.description}"
            _ <- committable.offset.commit
            endTime <- IO.realTime
            duration = (endTime - startTime)
            _ <- metrics.notificationCompleted("delivered")
            _ <- metrics.deliveryDuration("delivered", duration)
          } yield ()
        }.handleErrorWith { e =>
          Stream.eval {
            for {
              _ <- error"Failed to process update: ${e.getMessage}"
              _ <- metrics.notificationCompleted("failed")
              endTime <- IO.realTime
              _ <- metrics.deliveryDuration("failed", 0.seconds)
            } yield ()
          }
        }
    }
  }

  def make(using repo: BotRepository[IO], lm: Logging.Make[IO], config: AppConfig, metrics: BotMetrics[IO]): Resource[IO, UpdateConsumer[IO]] = {
    given Deserializer[IO, UUID] = Deserializer.uuid
    given Deserializer[IO, LinkUpdate] = Deserializer.lift[IO, LinkUpdate] { bytes =>
      IO.delay {
        val json = new String(bytes, "UTF-8")
        json.jsonAs[LinkUpdate] match {
          case Left(error)  => throw new RuntimeException(s"Failed to parse LinkUpdate: $error")
          case Right(value) => value
        }
      }
    }

    val settings = ConsumerSettings[IO, UUID, LinkUpdate]
      .withAutoOffsetReset(AutoOffsetReset.Earliest)
      .withBootstrapServers(config.kafka.servers)
      .withGroupId(config.kafka.consumerGroup)
      .withEnableAutoCommit(false)

    KafkaConsumer
      .resource(settings)
      .evalTap(_.subscribeTo(config.kafka.topic))
      .map(Impl.apply)
  }
}
