package config

import cats.effect.IO
import com.comcast.ip4s.{Host, Port}
import config.kafka.KafkaConfig
import config.repository.DatabaseConfig
import outbox.OutboxConfig
import pureconfig.{ConfigReader, ConfigSource}
import sttp.client3.UriContext
import sttp.model.Uri

final case class AppConfig(
    token: String,
    scrapperUrl: Uri,
    database: DatabaseConfig,
    kafka: KafkaConfig,
    outbox: OutboxConfig,
) derives ConfigReader

object AppConfig:
  def load: IO[AppConfig] =
    IO.delay(ConfigSource.default.loadOrThrow[AppConfig])

  given ConfigReader[Uri] = ConfigReader[String].map(str => uri"$str")
