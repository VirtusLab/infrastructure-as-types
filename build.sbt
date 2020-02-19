name := "infrastructure-as-types"

version := "0.1"

organization := "com.virtuslab"

scalaVersion := "2.12.10"

scalacOptions += "-Ypartial-unification"

libraryDependencies ++= Seq(
  "com.beachape" %% "enumeratum" % "1.5.15",
  "org.typelevel" %% "cats-core" % "2.0.0",
  "io.skuber" %% "skuber" % "2.4.0",
  "org.scalatest" %% "scalatest" % "3.1.0" % Test
)

scalafmtOnCompile := true
