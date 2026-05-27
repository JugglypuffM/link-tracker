package http.clients

import cats.effect.IO
import domain.errors.HttpClientError
import domain.errors.HttpClientError.*
import domain.scrapper.CheckResult.GitHubResult
import resilience.Resilience
import sttp.client3.*
import sttp.model.StatusCode
import tethys.*
import tethys.jackson.*
import tofu.logging.Logging
import tofu.syntax.logging.LoggingInterpolator

trait GitHubClient[F[_]] {
  def getRepoInfo(owner: String, repo: String): F[Option[GitHubResult]]
}

object GitHubClient {
  final private class Impl(using
                           resilience: Resilience,
                           client: SttpBackend[IO, Any],
                           lm: Logging.Make[IO]
  ) extends GitHubClient[IO] {
    given Logging[IO] = Logging.Make[IO].forService[GitHubClient[IO]]

    def getRepoInfo(owner: String, repo: String): IO[Option[GitHubResult]] =
      resilience.execute {
        for {
          _ <- info"Fetching github update on repo $owner/$repo"

          response <- client
            .send(
              emptyRequest
                .get(uri"https://api.github.com/repos/$owner/$repo")
                .header("accept", "application/vnd.github+json")
                .header("User-Agent", "link-tracker")
                .response(asStringAlways)
            )
            .handleErrorWith(e => IO.raiseError(ConnectionFailed(e)))

          update <- handleResponse(response, owner, repo)
        } yield Some(update)
      }.handleErrorWith {
        case CircuitOpen =>
          warn"Circuit breaker is open for repo $owner/$repo" >> IO.pure(None)
        case _: Transient =>
          warn"Transient error while fetching repo $owner/$repo" >> IO.pure(None)
        case e: Permanent =>
          errorCause"Permanent error while fetching repo $owner/$repo" (e) >> IO.pure(None)
        case e =>
          errorCause"Unexpected error while fetching repo $owner/$repo" (e) >> IO.pure(None)
      }

    private def handleResponse(response: Response[String], owner: String, repo: String): IO[GitHubResult] =
      response.code match {
        case StatusCode.Ok =>
          response.body.jsonAs[GitHubResult] match
            case Right(v) =>
              info"Fetching successful for $owner/$repo: ${v.toDescription}" >> IO(v)
            case Left(err) =>
              errorCause"Deserialization error for $owner/$repo" (err) >> IO.raiseError(DeserializationFailed(err))
        case code =>
          val error = HttpClientError.fromStatusCode(
            code.code,
            response.body,
            response.header("Retry-After")
          )
          IO.raiseError(error)
      }
  }

  def make(using Resilience, SttpBackend[IO, Any], Logging.Make[IO]): GitHubClient[IO] =
    Impl()
}
