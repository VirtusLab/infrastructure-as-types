import Build._
import sbt.Keys.scalacOptions

val diffsonPlayJson = "org.gnieh" %% "diffson-play-json" % "4.0.3"
val jacksonDataformat = "com.fasterxml.jackson.dataformat" % "jackson-dataformat-yaml" % "2.11.2"
val json4sJackson = "org.json4s" %% "json4s-jackson" % "3.6.9"
val logbackClassic = "ch.qos.logback" % "logback-classic" % "1.2.3"
val playJson = "com.typesafe.play" %% "play-json" % "2.9.0"
val quicklens = "com.softwaremill.quicklens" %% "quicklens" % "1.6.0"
val scalaCollectionCompat = "org.scala-lang.modules" %% "scala-collection-compat" % "2.1.6" cross CrossVersion.binary
val scalatest = "org.scalatest" %% "scalatest" % "3.2.0"
val silencerVersion = "1.7.1"
val silencerLib = "com.github.ghik" % "silencer-lib" % silencerVersion % Provided cross CrossVersion.full
val silencerPlugin = compilerPlugin("com.github.ghik" % "silencer-plugin" % silencerVersion cross CrossVersion.full)
val skuber = "io.skuber" %% "skuber" % "2.4.0"
val specs2Version = "4.10.2"
val specs2Core = "org.specs2" %% "specs2-core" % specs2Version
val specs2MatcherExtra = "org.specs2" %% "specs2-matcher-extra" % specs2Version
val sttpClientVersion = "2.0.9"
val sttpClientBackendZio = "com.softwaremill.sttp.client" %% "async-http-client-backend-zio" % sttpClientVersion
val sttpClientCore = "com.softwaremill.sttp.client" %% "core" % sttpClientVersion
val sttpClientJson4s = "com.softwaremill.sttp.client" %% "json4s" % sttpClientVersion

lazy val iatCore =
  module(id = "iat-core", directory = "core")
    .libraries(jacksonDataformat, quicklens)
    .libraries(scalatest % Test)

lazy val kubernetesClient =
  module(id = "kubernetes-client-scala", directory = "kubernetes")
    .libraries(sttpClientCore, sttpClientJson4s, json4sJackson)
    .libraries(scalatest % Test, specs2Core % Test, specs2MatcherExtra % Test)
    .settings(
      scalacOptions ++= Seq("-feature", "-language:higherKinds", "-unchecked"),
      scalacOptions in Test += "-Yrangepos"
    )

lazy val iatScalatest =
  module(id = "iat-scalatest", directory = "scalatest")
    .libraries(scalatest, json4sJackson, jacksonDataformat, playJson, diffsonPlayJson)
    .disablePublish

lazy val iatOpenapi =
  module(id = "iat-openapi", directory = "openapi")
    .libraries(scalaCollectionCompat)
    .dependsOn(iatCore, kubernetesClient, iatScalatest % "test->test")

lazy val iatSkuber =
  module(id = "iat-skuber", directory = "skuber")
    .libraries(jacksonDataformat, quicklens, scalaCollectionCompat, silencerLib, silencerPlugin, skuber)
    .dependsOn(iatCore, iatScalatest % "test->test")

lazy val iatExamples =
  module(id = "iat-examples", directory = "examples")
    .libraries(logbackClassic, sttpClientCore, sttpClientBackendZio)
    .dependsOn(iatCore, iatSkuber, iatOpenapi)
    .disablePublish

lazy val root =
  module(id = "infrastructure-as-types", directory = ".")
    .aggregate(iatCore, kubernetesClient, iatScalatest, iatOpenapi, iatSkuber, iatExamples)
    .dependsOn(iatCore, iatOpenapi, iatSkuber)

onChangedBuildSource in Global := ReloadOnSourceChanges
