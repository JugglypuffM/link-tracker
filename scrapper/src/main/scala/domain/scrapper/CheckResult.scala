package domain.scrapper

import sttp.tapir.Schema
import tethys.JsonReader

import java.time.ZonedDateTime

sealed trait CheckResult {
  def lastUpdate: ZonedDateTime
  def toDescription: String
}

object CheckResult {
  case class GitHubResult(updatedAt: ZonedDateTime) extends CheckResult derives JsonReader {
    override def lastUpdate: ZonedDateTime = updatedAt
    override def toDescription: String     = s"Repo updated at $lastUpdate"
  }
}
