name := "iat-infra-graphs"

version := "0.0.1-SNAPSHOT"

//ThisBuild / crossScalaVersions := Seq("2.11.12", "2.12.10", "2.13.1")

scalaVersion := "2.13.1"

crossVersion := CrossVersion.full

organization := "com.virtuslab"

libraryDependencies += "org.scala-lang" % "scala-compiler" % scalaVersion.value

libraryDependencies += "org.scalameta" % "semanticdb-scalac-core" % "4.3.10" cross CrossVersion.full
libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.9.4"
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"
libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2"

publishMavenStyle := true

pomIncludeRepository := {
  _ => false
}

autoScalaLibrary := false

makePom := makePom.dependsOn(assembly).value
packageBin in Compile := crossTarget.value / (assemblyJarName in assembly).value

PB.targets in Compile := Seq(
  scalapb.gen() -> (sourceManaged in Compile).value
)

licenses += ("MIT", url("http://opensource.org/licenses/MIT"))

bintrayOrganization := Some("virtuslab")
bintrayRepository := "graphbuddy"