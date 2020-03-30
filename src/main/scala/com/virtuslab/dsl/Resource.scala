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
    ApplicationSelector(
      expressions = Expressions(expressions: _*),
      protocols = AllProtocols
    )

  def namespaceLabeled(expressions: Expression*): NamespaceSelector =
    NamespaceSelector(
      expressions = Expressions(expressions: _*),
      protocols = AllProtocols
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

abstract class Selector extends HasShortDescription {
  def expressions: Expressions
  def protocols: Protocols
  override def asShortString: String = expressions.asShortString
}

case class NamespaceSelector(expressions: Expressions, protocols: Protocols) extends Selector

case class ApplicationSelector(expressions: Expressions, protocols: Protocols) extends Selector

case object DenySelector extends Selector {
  override def expressions: Expressions = Unselected
  override def protocols: Protocols = AllProtocols
}

case object AllowSelector extends Selector {
  override def expressions: Expressions = Unselected
  override def protocols: Protocols = AllProtocols
}

case object NoSelector extends Selector {
  override def expressions: Expressions = Unselected
  override def protocols: Protocols = AllProtocols
}

trait Protocol {
  def down: Protocol
}

case object AnyProtocol extends Protocol {
  override def down: Protocol = AnyProtocol
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

trait IPProtocol extends Protocol {
  def cidr: IP.CIDR
  // TODO except[CIDR], as another type?
}

trait UDPProtocol extends Protocol {
  def port: Port
}

trait TCPProtocol extends Protocol {
  def port: Port
}

trait HTTPProtocol extends Protocol {
  def method: HTTP.Method
  def path: HTTP.Path
}

case class IP(cidr: IP.CIDR, down: Protocol) extends IPProtocol
object IP {
  sealed trait CIDR {
    def ip: String
    def mask: Short
  }
  case class IPAddress(ip: String) extends CIDR {
    def mask: Short = 32
  }
  case class IPRange(ip: String, mask: Short) extends CIDR
  case object AllIPs extends CIDR {
    def ip: String = "0.0.0.0"
    def mask: Short = 0
  }

  def apply(cidr: CIDR): IP = new IP(cidr, AnyProtocol)
}

case class UDP(port: Port, down: Protocol) extends UDPProtocol
object UDP {
  def apply(port: Port): UDP = new UDP(port, AnyProtocol)
  def apply(number: Int): UDP = new UDP(Port(number), AnyProtocol)
}

case class TCP(port: Port, down: Protocol) extends TCPProtocol
object TCP {
  def apply(port: Port): TCP = new TCP(port, AnyProtocol)
  def apply(number: Int): TCP = new TCP(Port(number), AnyProtocol)
}

// TODO TLS

case class HTTP(
    method: HTTP.Method,
    path: HTTP.Path,
    down: Protocol)
  extends HTTPProtocol
object HTTP {
  sealed trait Method
  case class AMethod(method: String) extends Method
  case object AnyMethod extends Method

  sealed trait Path
  case class APath(path: String) extends Path
  case object AnyPath extends Path

  def apply(method: Method, path: Path): HTTP = new HTTP(method, path, AnyProtocol)
  def apply(port: Port): HTTP = new HTTP(AnyMethod, AnyPath, TCP(port, AnyProtocol))
}

// TODO HTTPS

sealed trait Protocols {
  def protocols: Set[Protocol]
}
case object AllProtocols extends Protocols {
  override def protocols: Set[Protocol] = Set()
}
object Protocols {
  case class SelectedProtocols(protocols: Set[Protocol]) extends Protocols
  def apply(protocols: Set[Protocol]): SelectedProtocols = SelectedProtocols(protocols)
  def apply(protocols: Protocol*): Protocols = Protocols(protocols.toSet)
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
