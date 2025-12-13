package http.clients

import cats.effect.{IO, Resource}
import domain.scrapper.CheckResult.GitHub.GitHubResult
import sttp.client3.*
import tethys.*
import tethys.jackson.*
import tofu.logging.Logging
import tofu.syntax.logging.LoggingInterpolator

trait GitHubClient[F[_]] {
  def getRepoInfo(owner: String, repo: String): F[GitHubResult]
}

object GitHubClient {
  final private class Impl(using client: SttpBackend[IO, Any], lm: Logging.Make[IO])
    extends GitHubClient[IO] {
    given Logging[IO] = Logging.Make[IO].forService[GitHubClient[IO]]

    def getRepoInfo(owner: String, repo: String): IO[GitHubResult] =
      for {
        _ <- infoWith"Fetching github update" ("link" -> s"https://github.com/$owner/$repo")

        response <- client.send(
          emptyRequest
            .get(uri"https://api.github.com/repos/$owner/$repo")
            .header("accept", "application/vnd.github+json")
            .response(asStringAlways)
        )

        update <- response.body.jsonAs[GitHubResult] match
          case Right(v) =>
            infoWith"Fetching success" ("link" -> s"https://github.com/$owner/$repo", "update" -> v.lastUpdate)
              >> IO(v)
          case Left(err) =>
            errorCauseWith"Deserialization error" (err)("link" -> s"https://github.com/$owner/$repo")
              >> IO.raiseError(err)
      } yield update
  }
  
  def make(using SttpBackend[IO, Any], Logging.Make[IO]): GitHubClient[IO] = Impl()
}
