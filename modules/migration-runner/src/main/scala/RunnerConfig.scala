import cats.effect.IO
import pureconfig.{ConfigReader, ConfigSource}

final case class RunnerConfig(
    url: String,
    username: String,
    password: String,
    migrationsDirectory: String
) derives ConfigReader

object RunnerConfig {
  def load: IO[RunnerConfig] =
    IO.delay(ConfigSource.default.loadOrThrow[RunnerConfig])
}
