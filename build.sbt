ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.5"

val bot      = project.in(file("modules/bot")).settings(libraryDependencies ++= Dependencies.allDeps)
val scrapper = project.in(file("modules/scrapper")).settings(libraryDependencies ++= Dependencies.allDeps)
val migrationRunner = project.in(file("modules/migration-runner")).settings(libraryDependencies ++= Dependencies.allDeps)