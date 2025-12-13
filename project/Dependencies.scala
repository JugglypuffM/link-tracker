import sbt.*

object Dependencies {
  // cats
  val catsCore   = "org.typelevel" %% "cats-core"   % "2.13.0"
  val catsEffect = "org.typelevel" %% "cats-effect" % "3.6.3"

  // tapir
  val tapirVersion = "1.13.3"

  val tapirHttp4s     = "com.softwaremill.sttp.tapir" %% "tapir-http4s-server"     % tapirVersion
  val tapirSwagger    = "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % tapirVersion
  val tapirTethys     = "com.softwaremill.sttp.tapir" %% "tapir-json-tethys"       % tapirVersion
  val tapirSttpClient = "com.softwaremill.sttp.tapir" %% "tapir-sttp-client"       % tapirVersion

  // http4s
  val http4sVersion = "0.23.33"

  val http4sServer = "org.http4s" %% "http4s-ember-server" % http4sVersion
  val http4sDsl    = "org.http4s" %% "http4s-dsl"          % http4sVersion

  // sttp
  val sttpVersion = "3.11.0"

  val sttpCore = "com.softwaremill.sttp.client3" %% "core" % sttpVersion
  val sttpCats = "com.softwaremill.sttp.client3" %% "cats" % sttpVersion

  // logback
  val logback = "ch.qos.logback" % "logback-classic" % "1.5.22"

  // tethys
  val tethysVersion = "0.29.7"

  val tethysCore       = "com.tethys-json" %% "tethys-core"       % tethysVersion
  val tethysJackson    = "com.tethys-json" %% "tethys-jackson213" % tethysVersion
  val tethysDerivation = "com.tethys-json" %% "tethys-derivation" % tethysVersion

  // pureconfig
  val pureConfigVersion = "0.17.9"

  val pureConfigCore = "com.github.pureconfig" %% "pureconfig-core" % pureConfigVersion

  // canoe
  val canoeVersion = "0.6.0"

  val canoe = "org.augustjune" %% "canoe" % canoeVersion

  // tofu
  val tofuVersion = "0.14.0"

  val tofuLogging = "tf.tofu" %% "tofu-logging" % tofuVersion
  val tofuLoggingDerivation = "tf.tofu" %% "tofu-logging-derivation" % tofuVersion
  val tofuCore = "tf.tofu" %% "tofu-core-ce3" % tofuVersion

  val allDeps: Seq[ModuleID] = Seq(
    catsCore,
    catsEffect,
    tapirHttp4s,
    tapirSwagger,
    tapirTethys,
    tapirSttpClient,
    http4sServer,
    http4sDsl,
    sttpCore,
    sttpCats,
    logback,
    tethysCore,
    tethysJackson,
    tethysDerivation,
    pureConfigCore,
    canoe,
    tofuLogging,
    tofuLoggingDerivation,
    tofuCore,
  )
}
