package com.virtuslab.dsl

import java.net.URL

sealed trait PingAction
case class HttpPing(url: URL) extends PingAction
case class TCPPing(port: Int) extends PingAction

sealed trait HealthCheckAction
case class HttpHealthCheck(url: URL) extends HealthCheckAction
case class TCPHealthCheck(port: Int) extends HealthCheckAction

trait Application extends Labeled with Containerized with Networked

object Application {
  final case class ApplicationDefinition(
      labels: Labels,
      namespace: Namespace,
      configurations: List[Configuration],
      image: String,
      command: List[String],
      args: List[String],
      envs: List[Containerized.EnvironmentVariable],
      ports: List[Networked.Port],
      ping: Option[HttpPing],
      healthCheck: Option[HttpHealthCheck])
    extends Application
    with Namespaced

  final case class ApplicationReference(
      labels: Labels,
      configurations: List[Configuration],
      image: String,
      command: List[String],
      args: List[String],
      envs: List[Containerized.EnvironmentVariable],
      ports: List[Networked.Port],
      ping: Option[HttpPing],
      healthCheck: Option[HttpHealthCheck])
    extends Application
    with Transformable[ApplicationReference]
    with Definable[Application, ApplicationReference, ApplicationDefinition] {
    def define(implicit builder: NamespaceBuilder): ApplicationDefinition = {
      ApplicationDefinition(
        labels = labels,
        namespace = builder.namespace,
        configurations = configurations,
        image = image,
        command = command,
        args = args,
        envs = envs,
        ports = ports,
        ping = ping,
        healthCheck = healthCheck
      )
    }
  }

  def ref(
      labels: Labels,
      image: String,
      configurations: List[Configuration] = Nil,
      command: List[String] = Nil,
      args: List[String] = Nil,
      envs: List[Containerized.EnvironmentVariable] = Nil,
      ports: List[Networked.Port] = Nil,
      ping: Option[HttpPing] = None,
      healthCheck: Option[HttpHealthCheck] = None
    )(implicit
      builder: SystemBuilder
    ): ApplicationReference = {
    val app = ApplicationReference(
      labels = labels,
      configurations = configurations,
      image = image,
      command = command,
      args = args,
      envs = envs,
      ports = ports,
      ping = ping,
      healthCheck = healthCheck
    )
    builder.references(app)
    app
  }

  def apply(
      labels: Labels,
      image: String,
      configurations: List[Configuration] = Nil,
      command: List[String] = Nil,
      args: List[String] = Nil,
      envs: List[Containerized.EnvironmentVariable] = Nil,
      ports: List[Networked.Port] = Nil,
      ping: Option[HttpPing] = None,
      healthCheck: Option[HttpHealthCheck] = None
    )(implicit
      builder: NamespaceBuilder
    ): ApplicationDefinition = {
    ref(
      labels = labels,
      configurations = configurations,
      image = image,
      command = command,
      args = args,
      envs = envs,
      ports = ports,
      ping = ping,
      healthCheck = healthCheck
    )(builder.systemBuilder).define
  }
}
