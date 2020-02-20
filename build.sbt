name := "infrastructure-as-types"

version := "0.1"

organization := "com.virtuslab"

scalaVersion := "2.12.10"

libraryDependencies ++= Seq(
  "io.skuber" %% "skuber" % "2.4.0",
  "io.kubernetes" % "client-java" % "6.0.1",
  "com.google.auth" % "google-auth-library-oauth2-http" % "0.20.0",
  "ch.qos.logback" % "logback-classic" % "1.1.3" % Runtime
)
