package domain.scrapper

import domain.link.Link
import sttp.tapir.Schema
import tethys.JsonReader

import java.time.ZonedDateTime

case class CheckInfo(link: Link, result: CheckResult)
