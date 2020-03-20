package com.virtuslab.dsl

import java.net.URL

import cats.Show

trait Containerized {
  def image: String
  def command: List[String]
  def args: List[String]
  def envs: List[Containerized.EnvironmentVariable]
}

object Containerized {
  case class EnvironmentVariable(key: String, value: String)
}

trait Networked {
  def ports: List[Networked.Port]
  def ping: Option[HttpPing]
  def healthCheck: Option[HttpHealthCheck]
}

object Networked {
  case class Port(number: Int, name: Option[String] = None)

  object Port {
    implicit val show: Show[Port] = Show.show { port =>
      s"Port(number=${port.number}, name=${port.name}"
    }
  }
}

sealed trait PingAction
case class HttpPing(url: URL) extends PingAction
case class TCPPing(port: Int) extends PingAction

sealed trait HealthCheckAction
case class HttpHealthCheck(url: URL) extends HealthCheckAction
case class TCPHealthCheck(port: Int) extends HealthCheckAction

trait Configuration extends Reference

object Configuration {
  final case class ConfigurationDefinition(
      labels: Labels,
      namespace: Namespace,
      data: Map[String, String])
    extends Configuration
    with Namespaced

  final case class ConfigurationReference(labels: Labels, data: Map[String, String]) extends Configuration {
    def define(implicit builder: NamespaceBuilder): ConfigurationDefinition = {
      ConfigurationDefinition(
        labels = labels,
        namespace = builder.namespace,
        data = data
      )
    }
  }

  def ref(
      labels: Labels,
      data: Map[String, String]
    )(implicit
      builder: SystemBuilder
    ): ConfigurationReference = {
    val conf = ConfigurationReference(
      labels = labels,
      data = data
    )
    builder.references(conf)
    conf
  }

  def apply(
      labels: Labels,
      data: Map[String, String]
    )(implicit
      builder: NamespaceBuilder
    ): ConfigurationDefinition = {
    val conf = ref(
      labels = labels,
      data = data
    )(builder.systemBuilder).define
    builder.references(conf)
    conf
  }
}

trait Application extends Reference with Containerized with Networked

object Application {
  final case class ApplicationDefinition private (
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

  final case class ApplicationReference private (
      labels: Labels,
      configurations: List[Configuration],
      image: String,
      command: List[String],
      args: List[String],
      envs: List[Containerized.EnvironmentVariable],
      ports: List[Networked.Port],
      ping: Option[HttpPing],
      healthCheck: Option[HttpHealthCheck])
    extends Application {
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
    val app = ref(
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
    builder.applications(app)
    app
  }
}
