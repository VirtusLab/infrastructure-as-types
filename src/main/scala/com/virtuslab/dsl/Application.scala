package com.virtuslab.dsl

import java.net.URL

import cats.Show
import cats.data.NonEmptyList
import cats.syntax.option._
import cats.syntax.show._

trait Namespace {
  def name: String
}

object Namespace {
  final case class UndefinedNamespace protected (name: String) extends Namespace {
    def inNamespace(f: Namespace => NonEmptyList[AnyRef]): DefinedNamespace = {
      DefinedNamespace(name, f(this))
    }
  }

  final case class DefinedNamespace protected (name: String, components: NonEmptyList[AnyRef]) extends Namespace

  def apply(name: String): UndefinedNamespace = {
    UndefinedNamespace(name)
  }
}

sealed trait PingAction
case class HttpPing(url: URL) extends PingAction
case class TCPPing(port: Int) extends PingAction

sealed trait HealthCheckAction
case class HttpHealthCheck(url: URL) extends HealthCheckAction
case class TCPHealthCheck(port: Int) extends HealthCheckAction

case class Configuration(
    name: String,
    namespace: Namespace,
    data: Map[String, String])

object Configuration {
  def apply(name: String, data: Map[String, String])(implicit ns: Namespace): Configuration = {
    Configuration(name, ns, data)
  }
}

object Application {
  case class Port(number: Int, name: Option[String] = None)
  object Port {
    implicit val show: Show[Port] = Show.show { port =>
      s"Port(number=${port.number}, name=${port.name}"
    }
  }

  case class EnvironmentVariable(key: String, value: String)
}

abstract class Application {
  def name: String
  def namespace: Namespace
  def image: String
  def ports: List[Application.Port]
  def envs: List[Application.EnvironmentVariable]
  def ping: Option[PingAction]
  def healthCheck: Option[HealthCheckAction]
  def command: List[String]
  def args: List[String]
  def configurations: List[Configuration]

  protected def addPort(port: Application.Port): Application

  def listensOn(number: Int): Application = {
    addPort(Application.Port(number))
  }

  def listensOn(number: Int, name: String): Application = {
    addPort(Application.Port(number, name.some))
  }
}

case class HttpApplication(
    name: String,
    image: String,
    namespace: Namespace,
    command: List[String],
    args: List[String],
    configurations: List[Configuration],
    ports: List[Application.Port],
    envs: List[Application.EnvironmentVariable],
    ping: Option[HttpPing],
    healthCheck: Option[HttpHealthCheck])
  extends Application {

  override protected def addPort(port: Application.Port): HttpApplication = {
    ports
      .find(_ == port)
      .fold {
        copy(ports = port :: ports)
      } { port =>
        throw new IllegalStateException(
          s"Port ${port.show} is already defined."
        )
      }
  }

  def withConfiguration(configuration: Configuration): HttpApplication = {
    copy(configurations = configuration :: configurations)
  }
}

object HttpApplication {
  def apply(
      name: String,
      image: String,
      command: List[String] = Nil,
      args: List[String] = Nil,
      configurations: List[Configuration] = Nil,
      ports: List[Application.Port] = Nil,
      envs: List[Application.EnvironmentVariable] = Nil,
      ping: Option[HttpPing] = None,
      healthCheck: Option[HttpHealthCheck] = None
    )(implicit
      ns: Namespace
    ): HttpApplication = {
    HttpApplication(name = name,
                    image = image,
                    namespace = ns,
                    command = command,
                    args = args,
                    configurations = configurations,
                    ports = ports,
                    envs = envs,
                    ping = ping,
                    healthCheck = healthCheck)
  }
}
