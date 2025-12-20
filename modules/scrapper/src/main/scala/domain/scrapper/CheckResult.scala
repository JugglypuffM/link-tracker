package domain.scrapper

import tethys.*

import java.time.Instant

sealed trait CheckResult {
  def lastUpdate: Instant
  def toDescription: String
}

object CheckResult {
  case class GitHubResult(updated_at: Instant) extends CheckResult derives JsonReader {
    override def lastUpdate: Instant   = updated_at
    override def toDescription: String = s"Repo updated at $lastUpdate"
  }
}
