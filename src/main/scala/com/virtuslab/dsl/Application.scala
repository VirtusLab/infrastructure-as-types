package com.virtuslab.dsl

import java.net.URL

sealed trait PingAction
case class HttpPing(url: URL) extends PingAction
case class TCPPing(port: Int) extends PingAction

sealed trait HealthCheckAction
case class HttpHealthCheck(url: URL) extends HealthCheckAction
case class TCPHealthCheck(port: Int) extends HealthCheckAction

case class Application(
    labels: Labels,
    image: String,
    configurations: List[Configuration] = Nil,
    command: List[String] = Nil,
    args: List[String] = Nil,
    envs: List[Containerized.EnvironmentVariable] = Nil,
    ports: List[Networked.Port] = Nil,
    ping: Option[HttpPing] = None,
    healthCheck: Option[HttpHealthCheck] = None,
    mounts: List[Mount] = Nil,
    secrets: List[Secret] = Nil)
  extends Labeled
  with Containerized
  with Networked
  with Mounts
  with Transformable[Application]
