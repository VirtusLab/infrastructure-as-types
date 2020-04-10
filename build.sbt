import sbt.Keys.scalacOptions

name := "infrastructure-as-types"

version := "0.1"

organization := "com.virtuslab"

scalaVersion := "2.12.10"

crossScalaVersions := Seq(scalaVersion.value,  "2.11.12")

scalacOptions += "-Ypartial-unification"

libraryDependencies ++= Seq(
  "com.beachape" %% "enumeratum" % "1.5.15",
  "org.typelevel" %% "cats-core" % "2.0.0",
  "io.skuber" %% "skuber" % "2.4.0",
  "com.fasterxml.jackson.dataformat" % "jackson-dataformat-yaml" % "2.10.1",
  "org.scalatest" %% "scalatest" % "3.1.0" % Test,
  "com.stephenn" %% "scalatest-play-json" % "0.0.3" % Test
)

scalafmtOnCompile := true

addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.11.0" cross CrossVersion.full)

lazy val kubernetes = project.in(file("kubernetes"))
  .settings(
    libraryDependencies ++= Seq(
      "com.google.code.gson" % "gson" % "2.8.6",
      "org.apache.commons" % "commons-lang3" % "3.7",
      "org.specs2" %% "specs2-core" % "4.8.3" % "test",
      "org.specs2" %% "specs2-matcher-extra" % "4.8.3" % "test",
    ),
    scalacOptions in Test ++= Seq("-Yrangepos")
  )

lazy val root = project.in(file("."))
  .aggregate(kubernetes)
  .dependsOn(kubernetes)
