package com.virtuslab.iat.dsl.kubernetes

import java.net.URL

import com.virtuslab.iat.dsl.{ Containerized, Label, Labeled, Named, Patchable, Port }

sealed trait PingAction
case class HttpPing(url: URL) extends PingAction
case class TCPPing(port: Int) extends PingAction

sealed trait HealthCheckAction
case class HttpHealthCheck(url: URL) extends HealthCheckAction
case class TCPHealthCheck(port: Int) extends HealthCheckAction

case class Application(
    labels: List[Label],
    image: String,
    configurations: List[Configuration] = Nil,
    secrets: List[Secret] = Nil,
    command: List[String] = Nil,
    args: List[String] = Nil,
    envs: List[Containerized.EnvironmentVariable] = Nil,
    ports: List[Port] = Nil,
    ping: Option[HttpPing] = None,
    healthCheck: Option[HttpHealthCheck] = None,
    mounts: List[Mount] = Nil)
  extends Named
  with Labeled
  with Containerized
  with Mounts
  with Patchable[Application]
