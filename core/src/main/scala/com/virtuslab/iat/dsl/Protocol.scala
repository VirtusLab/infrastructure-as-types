package com.virtuslab.iat.dsl

import com.virtuslab.iat.dsl.Port.NamedPort

trait Protocol

object Protocol {
  trait Layer7 extends Protocol
  object Layer7 {
    sealed trait Any extends Layer7
    case object Any extends Any
  }

  trait Layer4 extends Protocol
  object Layer4 {
    sealed trait Any extends Layer4
    case object Any extends Any
  }

  trait Layer3 extends Protocol
  object Layer3 {
    sealed trait Any extends Layer3
    case object Any extends Any
  }

  sealed trait Any extends Layer7 with Layer4 with Layer3
  case object Any extends Any

  trait HasCidr extends Protocol.Layer3 {
    def cidr: IP.CIDR
  }

  trait HasPort extends Protocol.Layer4 {
    def port: Port
  }

  trait IP extends Protocol.Layer3 with HasCidr
  trait UDP extends Protocol.Layer4 with HasPort
  trait TCP extends Protocol.Layer4 with HasPort

  trait TLS extends Protocol {
    def keyPairName: Option[String]
  }

  trait HTTP extends Protocol.Layer7 {
    def method: HTTP.Method
    def path: HTTP.Path
    def host: HTTP.Host
  }

  trait HTTPS extends Protocol.HTTP with Protocol.TLS {
    keyPairName: Option[String]
  }

  trait Layers[L7 <: Protocol.Layer7, L4 <: Protocol.Layer4, L3 <: Protocol.Layer3] {
    def l7: L7
    def l4: L4
    def l3: L3
  }

  case class SomeLayers[L7 <: Protocol.Layer7, L4 <: Protocol.Layer4, L3 <: Protocol.Layer3](
      l7: L7,
      l4: L4,
      l3: L3)
    extends Layers[L7, L4, L3]

  case object AnyLayers extends Layers[Protocol.Layer7, Protocol.Layer4, Protocol.Layer3] {
    override def l7: Protocol.Layer7.Any = Protocol.Layer7.Any
    override def l4: Protocol.Layer4.Any = Protocol.Layer4.Any
    override def l3: Protocol.Layer3.Any = Protocol.Layer3.Any
  }

  object Layers {
    def apply: Layers[Protocol.Layer7, Protocol.Layer4, Protocol.Layer3] = AnyLayers
    def apply[A <: Protocol.Layer7, B <: Protocol.Layer4, C <: Protocol.Layer3](
        l7: A = Protocol.Layer7.Any,
        l4: B = Protocol.Layer4.Any,
        l3: C = Protocol.Layer3.Any
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
  def apply: UDP = UDP(Port.Any)
  def apply(number: Int): UDP = UDP(Port(number))
  def apply(name: String, number: Int): UDP = UDP(NamedPort(name, number))
}

case class TCP(port: Port) extends Protocol.TCP
object TCP {
  def apply(): TCP = TCP(Port.Any)
  def apply(number: Int): TCP = TCP(Port(number))
  def apply(name: String, number: Int): TCP = TCP(NamedPort(name, number))
}

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

  def apply: HTTP = HTTP(Method.Any, Path.Any, Host.Any)
  def apply(
      method: Method = Method.Any,
      path: Path = Path.Any,
      host: Host = Host.Any
    ): HTTP = new HTTP(method, path, host)
}

case class HTTPS(
    method: HTTP.Method,
    path: HTTP.Path,
    host: HTTP.Host,
    keyPairName: Option[String])
  extends Protocol.HTTPS

object HTTPS {
  def apply: HTTPS = new HTTPS(
    HTTP.Method.Any,
    HTTP.Path.Any,
    HTTP.Host.Any,
    keyPairName = None
  )

  def apply(
      method: HTTP.Method = HTTP.Method.Any,
      path: HTTP.Path = HTTP.Path.Any,
      host: HTTP.Host = HTTP.Host.Any,
      keyPairName: Option[String] = None
    ): HTTPS = new HTTPS(method, path, host, keyPairName)
}

trait Protocols {
  def protocols: Set[Protocol.Layers[_ <: Protocol.Layer7, _ <: Protocol.Layer4, _ <: Protocol.Layer3]]
  def ports: Set[Protocol.HasPort] = protocols.map(_.l4).collect { case p: Protocol.HasPort => p }
  def cidrs: Set[Protocol.HasCidr] = protocols.map(_.l3).collect { case p: Protocol.HasCidr => p }
  def merge(other: Protocols): Protocols = Protocols.Selected(protocols ++ other.protocols)
}

object Protocols {
  sealed trait Any extends Protocols
  case object Any extends Any {
    override def protocols: Set[Protocol.Layers[_ <: Protocol.Layer7, _ <: Protocol.Layer4, _ <: Protocol.Layer3]] = Set()
  }
  sealed trait None extends Protocols
  case object None extends None {
    override def protocols: Set[Protocol.Layers[_ <: Protocol.Layer7, _ <: Protocol.Layer4, _ <: Protocol.Layer3]] = Set()
  }
  case class Selected(protocols: Set[Protocol.Layers[_ <: Protocol.Layer7, _ <: Protocol.Layer4, _ <: Protocol.Layer3]])
    extends Protocols

  def apply(protocols: Protocol.Layers[_ <: Protocol.Layer7, _ <: Protocol.Layer4, _ <: Protocol.Layer3]*): Protocols =
    apply(protocols)
  def apply(protocols: Seq[_ <: Protocol.Layers[_ <: Protocol.Layer7, _ <: Protocol.Layer4, _ <: Protocol.Layer3]]): Selected =
    Selected(protocols.toSet)

  def ports(ports: Protocol.HasPort*): Protocols = apply(ports.map(port => Protocol.Layers(l4 = port)))
  def cidrs(cidrs: Protocol.HasCidr*): Protocols = apply(cidrs.map(cidr => Protocol.Layers(l3 = cidr)))
}

sealed trait Port {
  def getNumber: Option[Int] = get.map(_._1)
  def getName: Option[String] = get.flatMap(_._2)
  def get: Option[(Int, Option[String])] = this match {
    case Port.Any                     => None
    case Port.APort(number)           => Some(number, None)
    case Port.NamedPort(name, number) => Some(number, Some(name))
  }
}
object Port {
  sealed trait Any extends Port
  case object Any extends Any
  case class APort(number: Int) extends Port
  case class NamedPort(name: String, number: Int) extends Port

  def apply(number: Int): Port = APort(number)
  def apply(name: String, number: Int): Port = NamedPort(name, number)
}
