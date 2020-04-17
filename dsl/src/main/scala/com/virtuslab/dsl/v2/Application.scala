package com.virtuslab.dsl.v2

final case class Namespace(name: String)

case class Configuration(name: String, data: Map[String, String])

object Configuration {
  implicit val materializer = Materializer[Configuration]
}

case class Secret(name: String, data: Map[String, String])

object Secret {
  implicit val materializer = Materializer[Secret]
}

case class Application(
    name: String,
    configurations: List[Configuration] = Nil,
    secrets: List[Secret] = Nil)

object Application {
  implicit val materializer = Materializer[Application]
}
