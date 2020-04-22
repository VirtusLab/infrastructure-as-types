package com.virtuslab.iat.dsl

case class Namespace(labels: List[Label]) extends Named with Labeled

case class Configuration(labels: List[Label], data: Map[String, String]) extends Named with Labeled

case class Secret(labels: List[Label], data: Map[String, String]) extends Named with Labeled

case class Application(
    labels: List[Label],
    configurations: List[Configuration] = Nil,
    secrets: List[Secret] = Nil)
  extends Named
  with Labeled

case class Gateway(labels: List[Label]) extends Named with Labeled
