package com.virtuslab.iat.dsl

trait Containerized {
  def image: String
  def command: List[String]
  def args: List[String]
  def envs: List[Containerized.EnvironmentVariable]
}

object Containerized {
  case class EnvironmentVariable(key: String, value: String)
}
