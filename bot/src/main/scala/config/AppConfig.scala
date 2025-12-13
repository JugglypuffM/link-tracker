package config

import cats.effect.IO
import pureconfig.*

final case class AppConfig(
    token: String,
) derives ConfigReader

object AppConfig:
  def load: IO[AppConfig] =
    IO.delay(ConfigSource.default.loadOrThrow[AppConfig])