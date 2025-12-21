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
import tethys.*
import tethys.jackson.*
import tofu.logging.Logging
import tofu.syntax.location.logging.LoggingInterpolator

trait UpdateConsumer[F[_]] {
  def listenUpdates: Stream[F, Unit]
}

object UpdateConsumer {
  private final case class Impl(
      consumer: KafkaConsumer[IO, Long, LinkUpdate]
  )(using c: TelegramClient[IO], config: AppConfig, lm: Logging.Make[IO]) extends UpdateConsumer[IO] {
    given Logging[IO] = Logging.Make[IO].forService[UpdateConsumer[IO]]

    override def listenUpdates: Stream[IO, Unit] = {
      consumer
        .stream
        .evalMap { committable =>
          for {
            update <- committable.record.value.pure[IO]
            _ <- info"Processing update on link ${update.url.toString} for ${update.ownerId}: ${update.description}"
            _ <- SendMessage(update.ownerId, update.toMessage).call
            _ <- info"Sent update on link ${update.url.toString} for ${update.ownerId}: ${update.description}"
            _ <- committable.offset.commit
          } yield ()
        }
    }
  }

  def make(using c: TelegramClient[IO], lm: Logging.Make[IO], config: AppConfig): Resource[IO, UpdateConsumer[IO]] = {
    given Deserializer[IO, Long] = Deserializer.long
    given Deserializer[IO, LinkUpdate] = Deserializer.lift[IO, LinkUpdate] { bytes =>
      IO.delay {
        val json = new String(bytes, "UTF-8")
        json.jsonAs[LinkUpdate] match {
          case Left(error)  => throw new RuntimeException(s"Failed to parse LinkUpdate: $error")
          case Right(value) => value
        }
      }
    }

    val settings = ConsumerSettings[IO, Long, LinkUpdate]
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
