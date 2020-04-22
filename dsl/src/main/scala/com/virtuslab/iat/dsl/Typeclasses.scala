package com.virtuslab.iat.dsl

import com.virtuslab.dsl.{ Labeled, Labels, Named }

final case class Namespace(name: String, labels: Labels) extends Named with Labeled

case class Configuration(name: String, data: Map[String, String])

case class Secret(name: String, data: Map[String, String])

case class Application(
    name: String,
    configurations: List[Configuration] = Nil,
    secrets: List[Secret] = Nil)
