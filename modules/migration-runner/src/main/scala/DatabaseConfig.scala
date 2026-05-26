import cats.effect.IO
import pureconfig.{ConfigReader, ConfigSource}

final case class DatabaseConfig(
    url: String,
    username: String,
    password: String,
    migrationsDirectory: String
) derives ConfigReader
