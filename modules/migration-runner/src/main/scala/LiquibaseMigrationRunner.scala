import cats.effect.*
import liquibase.Liquibase
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.DirectoryResourceAccessor

import java.io.File
import java.sql.DriverManager

object LiquibaseMigrationRunner extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    for {
      config <- RunnerConfig.load
      _ <- Resource
        .make(IO.delay {
          DriverManager.getConnection(config.url, config.username, config.password)
        })(conn =>
          IO.delay(conn.close()).handleErrorWith(_ => IO.unit)
        )
        .use { connection =>
          IO.delay {
            val database = DatabaseFactory.getInstance()
              .findCorrectDatabaseImplementation(new JdbcConnection(connection))

            val resourceAccessor = new DirectoryResourceAccessor(new File(config.migrationsDirectory))

            val liquibase = new Liquibase(
              "changelog.yaml",
              resourceAccessor,
              database
            )

            liquibase.update("")
          }
        }
    } yield ExitCode.Success
}
