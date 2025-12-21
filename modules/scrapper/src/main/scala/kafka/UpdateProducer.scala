package kafka

import cats.effect.{IO, Resource}
import config.AppConfig
import domain.scrapper.LinkUpdate
import fs2.kafka.*
import tethys.*
import tethys.jackson.jacksonTokenWriterProducer

trait UpdateProducer[F[_]] {
  def produce(update: LinkUpdate): F[Unit]
}

object UpdateProducer {
  private final case class Impl(producer: KafkaProducer[IO, Long, LinkUpdate])(using config: AppConfig)
    extends UpdateProducer[IO] {
    override def produce(update: LinkUpdate): IO[Unit] =
      val record = ProducerRecord(
        topic = config.kafka.topic,
        key = update.ownerId,
        value = update
      )

      producer.produce(ProducerRecords.one(record)).flatten.as(())
  }

  def make(using config: AppConfig): Resource[IO, UpdateProducer[IO]] = {
    given Serializer[IO, Long]       = Serializer.long
    given Serializer[IO, LinkUpdate] = Serializer.lift[IO, LinkUpdate](v => IO.delay(v.asJson.getBytes))

    val settings = ProducerSettings[IO, Long, LinkUpdate].withBootstrapServers(config.kafka.servers)

    KafkaProducer.resource(settings).map(Impl.apply)
  }
}
