ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.5"

val common = project.in(file("modules/common")).settings(libraryDependencies ++= Dependencies.allDeps)
val bot      = project.in(file("modules/bot")).enablePlugins(JavaAppPackaging).dependsOn(common).settings(libraryDependencies ++= Dependencies.allDeps)
val scrapper = project.in(file("modules/scrapper")).enablePlugins(JavaAppPackaging).dependsOn(common).settings(libraryDependencies ++= Dependencies.allDeps)
val migrationRunner = project.in(file("modules/migration-runner")).enablePlugins(JavaAppPackaging).settings(libraryDependencies ++= Dependencies.allDeps)