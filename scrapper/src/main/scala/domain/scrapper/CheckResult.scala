package domain.scrapper

import sttp.tapir.Schema
import tethys.JsonReader

import java.time.ZonedDateTime

sealed trait CheckResult(val lastUpdate: ZonedDateTime){
  def toDescription: String
}

object CheckResult{
  object GitHub{
    case class GitHubResult(lastUpdate: ZonedDateTime) extends CheckResult(lastUpdate) derives Schema {
      override def toDescription: String = s"Repo updated at $lastUpdate"
    }

    given JsonReader[GitHubResult] =
      JsonReader.builder
        .addField[ZonedDateTime]("updated_at")
        .buildReader(GitHubResult.apply)
  }
  
  object StackOverflow{
    case class StackOverflowResult(lastUpdate: ZonedDateTime) extends CheckResult(lastUpdate){
      override def toDescription: String = s"New answer at $lastUpdate"
    }
    
    case class StackOverflowAnswer(creationDate: ZonedDateTime) derives Schema

    given JsonReader[StackOverflowAnswer] =
      JsonReader.builder
        .addField[ZonedDateTime]("updated_at")
        .buildReader(StackOverflowAnswer.apply)
  }
}