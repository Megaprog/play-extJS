import sbt.Keys._

lazy val root = (project in file(".")).
  enablePlugins(PlayScala).
  settings(
    name := "play-extJS",
    version := "1.0",
    scalaVersion := "2.11.4",
    libraryDependencies ++= Seq(
      jdbc,
      anorm
    )
  )
