package com.virtuslab.dsl

import java.net.URL

import cats.Show

sealed trait PingAction
case class HttpPing(url: URL) extends PingAction
case class TCPPing(port: Int) extends PingAction

sealed trait HealthCheckAction
case class HttpHealthCheck(url: URL) extends HealthCheckAction
case class TCPHealthCheck(port: Int) extends HealthCheckAction

case class Configuration(
    name: String,
    namespace: Namespace,
    labels: Set[Label],
    data: Map[String, String])
  extends Resource
  with Namespaced
  with Labeled {
  def labeled(ls: Label*): Configuration = {
    copy(labels = labels ++ ls)
  }
}

object Configuration {
  def apply(name: String, data: Map[String, String])(implicit ns: Namespace): Configuration = {
    Configuration(name, ns, Set(NameLabel(name)), data)
  }
}

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
trait ResourceReference extends Resource with Labeled

trait Application extends ResourceReference with Containerized with Networked

object Application {
  case class ApplicationReference(
      name: String,
      labels: Set[Label],
      configurations: List[Configuration],
      image: String,
      command: List[String],
      args: List[String],
      envs: List[Containerized.EnvironmentVariable],
      ports: List[Networked.Port],
      ping: Option[HttpPing],
      healthCheck: Option[HttpHealthCheck])
    extends ResourceReference {
    def bind()(implicit namespace: Namespace): DefinedApplication = {
      DefinedApplication(
        name = name,
        labels = labels,
        namespace = namespace,
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

  case class DefinedApplication(
      name: String,
      labels: Set[Label],
      namespace: Namespace,
      configurations: List[Configuration],
      image: String,
      command: List[String],
      args: List[String],
      envs: List[Containerized.EnvironmentVariable],
      ports: List[Networked.Port],
      ping: Option[HttpPing],
      healthCheck: Option[HttpHealthCheck])
    extends Resource
    with Namespaced
    with Labeled
    with Containerized
    with Networked

  def apply(
      name: String,
      image: String,
      labels: Set[Label] = Set.empty,
      configurations: List[Configuration] = Nil,
      command: List[String] = Nil,
      args: List[String] = Nil,
      envs: List[Containerized.EnvironmentVariable] = Nil,
      ports: List[Networked.Port] = Nil,
      ping: Option[HttpPing] = None,
      healthCheck: Option[HttpHealthCheck] = None
    ): ApplicationReference = {
    ApplicationReference(
      name = name,
      labels = Set(NameLabel(name)) ++ labels,
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
