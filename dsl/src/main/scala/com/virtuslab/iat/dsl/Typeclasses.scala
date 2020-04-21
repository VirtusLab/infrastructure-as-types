package com.virtuslab.iat.dsl

final case class Namespace(name: String)

case class Configuration(name: String, data: Map[String, String])

case class Secret(name: String, data: Map[String, String])

case class Application(
    name: String,
    configurations: List[Configuration] = Nil,
    secrets: List[Secret] = Nil)
