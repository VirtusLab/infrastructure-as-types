import sbt.Keys.{scalacOptions, version}

val projectVersion = "0.3.5"
val projectScalaVersion = "2.13.1"
val projectOrganization = "com.virtuslab"

lazy val core = (project in file("core"))
  .settings(
    name := "iat-core",
    version := projectVersion,
    organization := projectOrganization,
    scalaVersion := projectScalaVersion,
    libraryDependencies ++= Seq(
      "com.fasterxml.jackson.dataformat" % "jackson-dataformat-yaml" % "2.10.1",
      "com.propensive" %% "magnolia" % "0.14.5",
      "com.softwaremill.quicklens" %% "quicklens" % "1.5.0",
      "org.scalatest" %% "scalatest" % "3.1.0" % Test,
    ),
    scalacOptions ++= Seq("-deprecation" /*"-Ymacro-debug-verbose"*/ /*"-Ymacro-debug-lite"*/),
    scalafmtOnCompile := true
  )

lazy val openapi = (project in file("openapi"))
  .settings(
    name := "iat-openapi",
    version := projectVersion,
    organization := projectOrganization,
    scalaVersion := projectScalaVersion,
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % "3.1.0" % Test,
    ),
    scalacOptions ++= Seq("-deprecation"),
    scalafmtOnCompile := true
  ).dependsOn(core, kubernetes, scalatest)

lazy val skuber = (project in file("skuber"))
  .settings(
    name := "iat-skuber",
    version := projectVersion,
    organization := projectOrganization,
    scalaVersion := projectScalaVersion,
    libraryDependencies ++= Seq(
      "io.skuber" %% "skuber" % "2.4.0",
      "com.fasterxml.jackson.dataformat" % "jackson-dataformat-yaml" % "2.10.1",
      "com.softwaremill.quicklens" %% "quicklens" % "1.5.0"
    ),
    scalacOptions ++= Seq("-deprecation"),
    scalafmtOnCompile := true
  ).dependsOn(core, scalatest)

lazy val examples = (project in file("examples"))
  .settings(
    name := "iat-examples",
    version := projectVersion,
    organization := projectOrganization,
    scalaVersion := projectScalaVersion,
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.client" %% "core" % "2.0.7",
      "com.softwaremill.sttp.client" %% "async-http-client-backend-zio" % "2.0.7",
      "ch.qos.logback" % "logback-classic" % "1.2.3",
    ),
    scalacOptions ++= Seq("-deprecation"),
    scalafmtOnCompile := true
  ).dependsOn(core, skuber, openapi)

lazy val scalatest = (project in file("scalatest"))
  .settings(
    name := "iat-scalatest",
    version := projectVersion,
    organization := projectOrganization,
    scalaVersion := projectScalaVersion,
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % "3.1.0",
      "org.json4s" %% "json4s-jackson" % "3.6.7",
      "com.fasterxml.jackson.dataformat" % "jackson-dataformat-yaml" % "2.10.1",
      "com.typesafe.play" %% "play-json" % "2.8.1",
      "org.gnieh" %% "diffson-play-json" % "4.0.2"
    ),
    scalacOptions ++= Seq("-deprecation"),
    scalafmtOnCompile := true
  )

lazy val kubernetes = (project in file("kubernetes"))
  .settings(
    name := "kubernetes-client-scala",
    version := projectVersion,
    organization := projectOrganization,
    scalaVersion := projectScalaVersion,
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.client" %% "core" % "2.0.0",
      "com.softwaremill.sttp.client" %% "json4s" % "2.0.0",
      "org.json4s" %% "json4s-jackson" % "3.6.7",
      // test dependencies
      "org.scalatest" %% "scalatest" % "3.0.8" % Test,
      "org.specs2" %% "specs2-core" % "4.8.3" % Test,
      "org.specs2" %% "specs2-matcher-extra" % "4.8.3" % Test,
    ),
    scalacOptions := Seq(
      "-language:higherKinds",
      "-unchecked",
      "-deprecation",
      "-feature"
    ),
    scalacOptions in Test ++= Seq("-Yrangepos"),
    scalafmtOnCompile := true
  )

lazy val root = (project in file("."))
  .settings(name := "infrastructure-as-types")
  .aggregate(core, kubernetes, skuber, examples)
