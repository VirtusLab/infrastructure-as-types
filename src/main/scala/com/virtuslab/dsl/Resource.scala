package com.virtuslab.dsl

import com.virtuslab.dsl.Expressions.Expression

trait Named { self: Labeled =>
  def name: String = labels.name.value
}

trait Transformable[A] { self: A =>
  def transform(f: A => A): A = f(self)
}

trait HasShortDescription {
  def asShortString: String
}

sealed trait Unselected extends Expressions
case object Unselected extends Unselected {
  override def expressions: Set[Expression] = Set()
  override def asShortString: String = "unselected"
}

trait Labeled extends Named with Expressions {
  def labels: Labels

  override def expressions: Set[Expression] = labels.all.toSet[Expression]
  override def asShortString: String = labels.asShortString

  override def hashCode(): Int = labels.hashCode()
  override def equals(obj: Any): Boolean = obj match {
    case other: Labeled => labels.equals(other.labels)
    case _              => false
  }
}

trait Expressions extends HasShortDescription {
  def expressions: Set[Expression]
}

object Expressions {
  def apply(expressions: Expression*): Expressions = ExpressionsDefinition(expressions.toSet)
  def apply(expressions: Set[Expression]): Expressions = ExpressionsDefinition(expressions)

  final case class ExpressionsDefinition(expressions: Set[Expression]) extends Expressions {
    override def toString: String = expressions.mkString(",")
    override def asShortString: String = toString.replaceAll("[!@#$%^&*()+=<>|/\\\\_,.]*", "-").take(20)
  }

  sealed trait Expression {
    val key: String
  }

  sealed trait EqualityExpression extends Expression {
    val value: String
  }

  sealed trait SetExpression extends Expression {
    val values: Seq[String]
    def valuesAsString: String = "(" + values.mkString(",") + ")"
  }

  case class ExistsExpression(key: String) extends Expression {
    override def toString: String = key
  }

  case class NotExistsExpression(key: String) extends Expression {
    override def toString: String = "!" + key
  }

  case class IsEqualExpression(key: String, value: String) extends EqualityExpression {
    override def toString: String = key + "=" + value
  }

  case class IsNotEqualExpression(key: String, value: String) extends EqualityExpression {
    override def toString: String = key + "!=" + value
  }

  case class InExpression(key: String, values: Seq[String]) extends SetExpression {
    override def toString: String = key + " in " + valuesAsString
  }

  case class NotInExpression(key: String, values: Seq[String]) extends SetExpression {
    override def toString: String = key + " notin " + valuesAsString
  }

  import scala.language.implicitConversions

  // this DSL enables equality and set based selector expressions analogous to the Kubernetes API
  // The following illustrates mappings from this DSL to k8s selector expressions syntax:
  // "production" -> "production"
  // '"production" doesNotExist ->  "!production"
  // "tier" is "frontend" -> "tier=frontend"
  // "status" isNot "release" -> "status!=release"
  // "env" isIn List("staging", "production") -> "env in (staging,release)"
  // "env" isNotIn List("local", "dev") -> "env notin (local,dev)"
  implicit def stringToExpression(key: String) = new ExistsExpression(key) {
    def doesNotExist: NotExistsExpression = NotExistsExpression(key)
    def is(value: String): IsEqualExpression = IsEqualExpression(key, value)
    def isNot(value: String): IsNotEqualExpression = IsNotEqualExpression(key, value)
    def in(values: String*): InExpression = InExpression(key, values)
    def isNotIn(values: List[String]): NotInExpression = NotInExpression(key, values)
  }

  def applicationLabeled(expressions: Expression*): ApplicationSelector =
    SelectedApplications(
      expressions = Expressions(expressions: _*),
      protocols = Protocols.Any
    )

  def namespaceLabeled(expressions: Expression*): NamespaceSelector =
    SelectedNamespaces(
      expressions = Expressions(expressions: _*),
      protocols = Protocols.Any
    )
}

trait Label extends Expressions.EqualityExpression {
  override def toString: String = key + "=" + value
}

final case class Name(value: String) extends Label {
  val key: String = "name"
}

final case class UntypedLabel(key: String, value: String) extends Label

case class Labels(name: Name, private val values: Set[Label]) extends Expressions {
  import scala.collection.generic.CanBuildFrom

  def all: Set[Label] = Set(name) ++ values
  def tail: Set[Label] = values
  def toMap: Map[String, String] = all.map(l => l.key -> l.value).toMap
  def map[B, That](f: Label => B)(implicit bf: CanBuildFrom[Set[Label], B, That]): That = all.map(f)(bf)

  override def expressions: Set[Expression] = all.map(l => l)
  override def asShortString: String = name.value.take(20)
}

object Labels {
  def apply(name: Name, values: Label*): Labels = Labels(name, values.toSet)
}

trait Selector extends HasShortDescription {
  def expressions: Expressions
  def protocols: Protocols
  override def asShortString: String = expressions.asShortString
}

sealed trait NamespaceSelector extends Selector
case class SelectedNamespaces(expressions: Expressions, protocols: Protocols) extends NamespaceSelector
case object AllNamespaces extends NamespaceSelector {
  override def expressions: Expressions = Expressions()
  override def protocols: Protocols = Protocols()
}

sealed trait ApplicationSelector extends Selector
case class SelectedApplications(expressions: Expressions, protocols: Protocols) extends ApplicationSelector
case object AllApplications extends ApplicationSelector {
  override def expressions: Expressions = Expressions()
  override def protocols: Protocols = Protocols()
}

case object DenySelector extends Selector {
  override def expressions: Expressions = Unselected
  override def protocols: Protocols = Protocols()
}

case object AllowSelector extends Selector {
  override def expressions: Expressions = Unselected
  override def protocols: Protocols = Protocols()
}

case object NoSelector extends Selector {
  override def expressions: Expressions = Unselected
  override def protocols: Protocols = Protocols()
}

case class SelectedIPs(ips: IP.CIDR*) extends Selector {
  override def expressions: Expressions = Unselected
  override def protocols: Protocols = Protocols.ip(ips.map(IP(_)): _*)
}

sealed trait ProtocolPort {
  def numberOrName: Either[Int, String]
  def asString: String = numberOrName.fold(_.toString, identity)
}
case class Port(numberOrName: Either[Int, String]) extends ProtocolPort
object Port {
  def apply(number: Int): Port = new Port(Left(number))
  def apply(name: String): Port = new Port(Right(name))
}
case object AllPorts extends ProtocolPort {
  override def numberOrName = Left(0)
}

trait Protocol {
  @deprecated
  def down: Protocol
}

object Protocol {
  trait L7 extends Protocol
  trait L4 extends Protocol
  trait L3 extends Protocol

  sealed trait Any extends L7 with L4 with L3
  case object Any extends Any {
    override def down: Protocol = Protocol.Any
  }

  trait IP extends Protocol.L3 {
    def cidr: IP.CIDR
  }

  trait UDP extends Protocol.L4 {
    def port: Port
  }

  trait TCP extends Protocol.L4 {
    def port: Port
  }

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

case class IP(cidr: IP.CIDR, down: Protocol) extends Protocol.IP
object IP {
  import scala.util.matching.Regex

  private[this] val cidrFmt: String = "(([0-9]{1,3}\\.){3}[0-9]{1,3})\\/([0-9]|[1-2][0-9]|3[0-2])?"
  private[this] val cidrRegexp: Regex = ("^" + cidrFmt + "$").r

  sealed trait CIDR {
    def ip: String
    def mask: Short
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

  def apply(cidr: CIDR): IP = new IP(cidr, Protocol.Any)
}

case class UDP(port: Port, down: Protocol) extends Protocol.UDP
object UDP {
  def apply(port: Port): UDP = new UDP(port, Protocol.Any)
  def apply(number: Int): UDP = new UDP(Port(number), Protocol.Any)
}

case class TCP(port: Port, down: Protocol) extends Protocol.TCP
object TCP {
  def apply(port: Port): TCP = new TCP(port, Protocol.Any)
  def apply(number: Int): TCP = new TCP(Port(number), Protocol.Any)
}

// TODO TLS

case class HTTP(
    method: HTTP.Method,
    path: HTTP.Path,
    host: HTTP.Host,
    down: Protocol)
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
    ): HTTP = new HTTP(method, path, host, Protocol.Any)
}

// TODO HTTPS

sealed trait Protocols {
  def protocols: Set[Protocol.Layers[_ <: Protocol.L7, _ <: Protocol.L4, _ <: Protocol.L3]]
}

object Protocols {
  case object Any extends Protocols {
    override def protocols: Set[Protocol.Layers[_ <: Protocol.L7, _ <: Protocol.L4, _ <: Protocol.L3]] = Set()
  }

  case class Selected(protocols: Set[Protocol.Layers[_ <: Protocol.L7, _ <: Protocol.L4, _ <: Protocol.L3]]) extends Protocols

  def apply(protocols: Protocol.Layers[_ <: Protocol.L7, _ <: Protocol.L4, _ <: Protocol.L3]*): Protocols =
    apply(protocols)
  def apply(protocols: Seq[_ <: Protocol.Layers[_ <: Protocol.L7, _ <: Protocol.L4, _ <: Protocol.L3]]): Selected =
    Selected(protocols.toSet)
//  def apply(protocols: Set[_ <: Protocol.Layers[_ <: Protocol.L7, _ <: Protocol.L4, _ <: Protocol.L3]]): Selected = Selected(protocols)
  def tcp(tcps: Protocol.TCP*): Protocols = apply(tcps.map(tcp => Protocol.Layers(l4 = tcp)))
  def ip(ips: Protocol.IP*): Protocols = apply(ips.map(ip => Protocol.Layers(l3 = ip)))
  def apply(): Protocols = Any
}

object Validation {
  // From Kubernetes:
  // https://github.com/kubernetes/kubernetes/blob/v1.18.0-rc.1/staging/src/k8s.io/apimachinery/pkg/util/validation/validation.go
  import scala.util.matching.Regex

  val qualifiedNameCharFmt: String = "[A-Za-z0-9]"
  val qualifiedNameExtCharFmt: String = "[-A-Za-z0-9_.]"
  val qualifiedNameFmt: String = "(" + qualifiedNameCharFmt + qualifiedNameExtCharFmt + "*)?" + qualifiedNameCharFmt
  val qualifiedNameMaxLength: Int = 63

  val qualifiedNameRegexp: Regex = ("^" + qualifiedNameFmt + "$").r

  def IsQualifiedName(value: String): (Boolean, String) = {
    if (value.length == 0) {
      return (false, "value is empty")
    }
    if (value.length > qualifiedNameMaxLength) {
      return (false, "value length exceeds " + qualifiedNameMaxLength + " characters")
    }
    if (!qualifiedNameRegexp.pattern.matcher(value).matches) {
      return (false, s"value doesn not match pattern '$qualifiedNameRegexp'")
    }
    (true, "")
  }
}
