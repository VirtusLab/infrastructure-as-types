import org.scalafmt.sbt.ScalafmtPlugin.autoImport.scalafmtOnCompile
import sbt.Keys._
import sbt.librarymanagement.ModuleID
import sbt.{ Project, file }

object Build {
  val scala212 = "2.12.11"
  val scala213 = "2.13.3"
  val projectScalaVersion = scala213
  val supportedScalaVersions = List(scala213, scala212)

  val projectVersion = "0.3.5-SNAPSHOT"
  val projectOrganization = "com.virtuslab"

  val commonScalacOptions = Seq(
    "-deprecation", "-feature", "-unchecked", "-Xfatal-warnings", "-Ybackend-parallelism", "8", "-Ywarn-dead-code", "-Ywarn-extra-implicit"
  )
  val lintScalacOptions = {
    "doc-detached inaccessible infer-any missing-interpolator nullary-unit private-shadow stars-align type-parameter-shadow"
      .split(" ").map("-Xlint:" + _)
  }
  val productionOnlyScalacOptions = Seq("-Ywarn-value-discard")

  val commonSettings = Seq(
    version := projectVersion,
    crossScalaVersions := supportedScalaVersions,
    organization := projectOrganization,
    scalaVersion := projectScalaVersion,
    scalacOptions ++= commonScalacOptions ++ lintScalacOptions,
    scalacOptions in compile ++= productionOnlyScalacOptions,
    scalafmtOnCompile := true
  )

  def module(id: String, directory: String): Project = {
    Project(id = id, base = file(directory))
      .settings(commonSettings: _*)
  }

  implicit class ProjectOps(project: Project) {
    def libraries(modules: ModuleID*): Project = {
      project.settings(libraryDependencies ++= modules)
    }

    def disablePublish: Project = {
      project.settings(skip in publish := true)
    }
  }
}
