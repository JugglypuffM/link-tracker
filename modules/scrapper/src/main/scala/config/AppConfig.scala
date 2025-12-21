package config

import cats.effect.IO
import com.comcast.ip4s.{Host, Port}
import config.kafka.KafkaConfig
import config.repository.DatabaseConfig
import pureconfig.{ConfigReader, ConfigSource}
import sttp.client3.UriContext
import sttp.model.Uri

final case class AppConfig(
    host: Host,
    port: Port,
    database: DatabaseConfig,
    kafka: KafkaConfig,
) derives ConfigReader

object AppConfig:
  def load: IO[AppConfig] =
    IO.delay(ConfigSource.default.loadOrThrow[AppConfig])

  given ConfigReader[Host] = ConfigReader[String].map(str => Host.fromString(str).get)
  given ConfigReader[Port] = ConfigReader[Int].map(i => Port.fromInt(i).get)
