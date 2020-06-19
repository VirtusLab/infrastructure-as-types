import org.scalafmt.sbt.ScalafmtPlugin.autoImport.scalafmtOnCompile
import sbt.Keys._
import sbt.librarymanagement.ModuleID
import sbt.{ Project, file }

object Build {
  val scala212 = "2.12.11"
  val scala213 = "2.13.2"
  val projectScalaVersion = scala213
  val supportedScalaVersions = List(scala213, scala212)

  val projectVersion = "0.3.5-SNAPSHOT"
  val projectOrganization = "com.virtuslab"

  val commonSettings = Seq(
    version := projectVersion,
    crossScalaVersions := supportedScalaVersions,
    organization := projectOrganization,
    scalaVersion := projectScalaVersion,
    scalacOptions += "-deprecation",
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
      project.settings(publishLocal := {}, publish := {})
    }
  }
}
