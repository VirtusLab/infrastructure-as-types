package com.virtuslab.iat.dsl

trait Containerized {
  def containers: List[Container]
}

trait Container extends Named with Labeled with Patchable[Container] {
  def labels: List[Label]
  def image: String
  def command: List[String]
  def args: List[String]
  def envs: List[EnvironmentVariable]
}

trait EnvironmentVariable {
  def key: String
  def value: String
}

object EnvironmentVariable {
  case class AnEnvironmentVariable(key: String, value: String) extends EnvironmentVariable
  def apply(key: String, value: String): EnvironmentVariable = AnEnvironmentVariable(key, value)
}
