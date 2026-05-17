import cats.effect.IO
import pureconfig.{ConfigReader, ConfigSource}

case class RunnerConfig(
    bot: DatabaseConfig,
    scrapper: DatabaseConfig,
) derives ConfigReader

object RunnerConfig {
  def load: IO[RunnerConfig] =
    IO.delay(ConfigSource.default.loadOrThrow[RunnerConfig])
}
