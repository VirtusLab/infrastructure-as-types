import sbt.Keys.scalacOptions

lazy val kubernetes = (project in file("kubernetes"))
  .settings(
    libraryDependencies ++= Seq(
      "org.specs2" %% "specs2-core" % "4.8.3" % "test",
      "org.specs2" %% "specs2-matcher-extra" % "4.8.3" % "test",
    ),
    scalacOptions ++= Seq("-language:higherKinds", "-deprecation"),
    scalacOptions in Test ++= Seq("-Yrangepos"),
    scalafmtOnCompile := true
  )

lazy val dsl = (project in file("dsl"))
  .settings(addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.11.0" cross CrossVersion.full))
  .settings(
    name := "dsl",
    version := "0.1",
    organization := "com.virtuslab",
    scalaVersion := "2.13.1",
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.client" %% "core" % "2.0.7",
      "com.softwaremill.sttp.client" %% "async-http-client-backend-zio" % "2.0.7",
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "io.skuber" %% "skuber" % "2.4.0",
      "com.fasterxml.jackson.dataformat" % "jackson-dataformat-yaml" % "2.10.1",
      "com.chuusai" %% "shapeless" % "2.3.3",
      "com.propensive" %% "magnolia" % "0.14.5",
      "com.softwaremill.quicklens" %% "quicklens" % "1.5.0",
      "org.scalatest" %% "scalatest" % "3.1.0" % Test,
      "com.stephenn" %% "scalatest-play-json" % "0.0.3" % Test
    ),
    scalacOptions ++= Seq("-deprecation" /*"-Ymacro-debug-verbose"*/ /*"-Ymacro-debug-lite"*/),
    scalafmtOnCompile := true
  ).dependsOn(kubernetes)

lazy val root = (project in file("."))
  .settings(name := "infrastructure-as-types")
  .aggregate(dsl, kubernetes)
