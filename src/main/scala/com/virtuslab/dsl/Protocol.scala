package com.virtuslab.dsl

trait Protocol

object Protocol {
  trait L7 extends Protocol
  trait L4 extends Protocol
  trait L3 extends Protocol

  sealed trait Any extends L7 with L4 with L3
  case object Any extends Any

  trait HasCidr extends Protocol.L3 {
    def cidr: IP.CIDR
  }

  trait HasPort extends Protocol.L4 {
    def port: Port
  }

  trait IP extends Protocol.L3 with HasCidr
  trait UDP extends Protocol.L4 with HasPort
  trait TCP extends Protocol.L4 with HasPort

  trait HTTP extends Protocol.L7 {
    def method: HTTP.Method
    def path: HTTP.Path
  }

  trait Layers[L7 <: Protocol.L7, L4 <: Protocol.L4, L3 <: Protocol.L3] {
    def l7: L7
    def l4: L4
    def l3: L3
  }

  case class SomeLayers[L7 <: Protocol.L7, L4 <: Protocol.L4, L3 <: Protocol.L3](
      l7: L7,
      l4: L4,
      l3: L3)
    extends Layers[L7, L4, L3]

  case object AnyLayers extends Layers[Protocol.Any, Protocol.Any, Protocol.Any] {
    override def l7: Protocol.Any = Protocol.Any
    override def l4: Protocol.Any = Protocol.Any
    override def l3: Protocol.Any = Protocol.Any
  }

  object Layers {
    def apply(): Layers[Protocol.Any, Protocol.Any, Protocol.Any] = AnyLayers
    def apply[A <: L7, B <: L4, C <: L3](
        l7: A = Protocol.Any,
        l4: B = Protocol.Any,
        l3: C = Protocol.Any
      ): Layers[A, B, C] = SomeLayers(l7, l4, l3)
  }
}

case class IP(cidr: IP.CIDR) extends Protocol.IP
object IP {
  import scala.util.matching.Regex

  private[this] val cidrFmt: String = "(([0-9]{1,3}\\.){3}[0-9]{1,3})\\/([0-9]|[1-2][0-9]|3[0-2])?"
  private[this] val cidrRegexp: Regex = ("^" + cidrFmt + "$").r

  sealed trait CIDR extends Protocol.HasCidr {
    def ip: String
    def mask: Short
    override def cidr: CIDR = this
  }
  case class Address(ip: String) extends CIDR {
    def mask: Short = 32
  }
  case class Range(ip: String, mask: Short) extends CIDR {
    def except(exceptions: CIDR*): RangeWithExceptions = RangeWithExceptions(ip, mask, exceptions.toSet)
  }
  object Range {
    def apply(cidr: String): Range = cidr match {
      case cidrRegexp(ip, _, mask) => Range(ip, mask.toShort)
    }
  }
  case class RangeWithExceptions(
      ip: String,
      mask: Short,
      exceptions: Set[CIDR])
    extends CIDR
  case object All extends CIDR {
    def ip: String = "0.0.0.0"
    def mask: Short = 0
  }
}

case class UDP(port: Port) extends Protocol.UDP
object UDP {
  def apply(number: Int): UDP = UDP(Port(number))
}

case class TCP(port: Port) extends Protocol.TCP
object TCP {
  def apply(number: Int): TCP = TCP(Port(number))
}

// TODO TLS

case class HTTP(
    method: HTTP.Method,
    path: HTTP.Path,
    host: HTTP.Host)
  extends Protocol.HTTP
object HTTP {
  sealed trait Method {
    def get: Option[String] = this match {
      case Method.Any             => None
      case Method.AMethod(method) => Some(method)
    }
  }
  object Method {
    case object Any extends Method
    case class AMethod(method: String) extends Method
    def apply(method: String): Method = AMethod(method)
  }

  sealed trait Path {
    def get: Option[String] = this match {
      case Path.Any         => None
      case Path.APath(path) => Some(path)
    }
  }
  object Path {
    case object Any extends Path
    case class APath(path: String) extends Path
    def apply(path: String): Path = APath(path)
  }

  sealed trait Host {
    def get: Option[String] = this match {
      case Host.Any         => None
      case Host.AHost(host) => Some(host)
    }
  }
  object Host {
    case object Any extends Host
    case class AHost(host: String) extends Host
    def apply(host: String): Host = AHost(host)
  }

  def apply(
      method: Method = Method.Any,
      path: Path = Path.Any,
      host: Host = Host.Any
    ): HTTP = new HTTP(method, path, host)
}

// TODO HTTPS

trait Protocols {
  def protocols: Set[Protocol.Layers[_ <: Protocol.L7, _ <: Protocol.L4, _ <: Protocol.L3]]
  def merge(other: Protocols): Protocols = Protocols.Selected(protocols ++ other.protocols)
}

object Protocols {
  sealed trait Any extends Protocols
  case object Any extends Any {
    override def protocols: Set[Protocol.Layers[_ <: Protocol.L7, _ <: Protocol.L4, _ <: Protocol.L3]] = Set()
  }

  case class Selected(protocols: Set[Protocol.Layers[_ <: Protocol.L7, _ <: Protocol.L4, _ <: Protocol.L3]]) extends Protocols

  def apply(protocols: Protocol.Layers[_ <: Protocol.L7, _ <: Protocol.L4, _ <: Protocol.L3]*): Protocols =
    apply(protocols)
  def apply(protocols: Seq[_ <: Protocol.Layers[_ <: Protocol.L7, _ <: Protocol.L4, _ <: Protocol.L3]]): Selected =
    Selected(protocols.toSet)
  //  def apply(protocols: Set[_ <: Protocol.Layers[_ <: Protocol.L7, _ <: Protocol.L4, _ <: Protocol.L3]]): Selected = Selected(protocols)
  def port(ports: Protocol.HasPort*): Protocols = apply(ports.map(port => Protocol.Layers(l4 = port)))
  def cidr(cidrs: Protocol.HasCidr*): Protocols = apply(cidrs.map(cidr => Protocol.Layers(l3 = cidr)))
  def apply(): Protocols = Any
}

sealed trait Port {
  def numberOrName: Either[Int, String]
  def asString: String = numberOrName.fold(_.toString, identity)
}
object Port {
  sealed trait Any extends Port
  case object Any extends Any {
    override def numberOrName = Left(0)
  }
  // TODO do we need "nameable ports" or can we just enforce integer ports?
  case class APort(numberOrName: Either[Int, String]) extends Port

  def apply(number: Int): Port = APort(Left(number))
  def apply(name: String): Port = APort(Right(name))
}

trait CIDRs {
  def ips: Seq[Protocol.HasCidr]
}
trait Ports {
  def ports: Seq[Protocol.HasPort]
}
