package com.virtuslab.dsl

import cats.Show

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
