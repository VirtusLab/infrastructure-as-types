package com.virtuslab.dsl

import com.virtuslab.dsl.Expressions.Expression

trait Transformable[A] { self: A =>
  def transform(f: A => A): A = f(self)
}

trait Named { self: Labeled =>
  def name: String = labels.name.value
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

trait HasShortDescription {
  def asShortString: String
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
  override def expressions: Expressions = Expressions.Any
  override def protocols: Protocols = Protocols()
}

case object AllowSelector extends Selector {
  override def expressions: Expressions = Expressions.Any
  override def protocols: Protocols = Protocols()
}

case object NoSelector extends Selector {
  override def expressions: Expressions = Expressions.Any
  override def protocols: Protocols = Protocols()
}

case class SelectedIPs(ips: Protocol.HasCidr*) extends Selector with CIDRs {
  override def expressions: Expressions = Expressions.Any
  override def protocols: Protocols = Protocols.cidr(ips: _*)

  def ports(ports: Protocol.HasPort*): SelectedIPsAndPorts = SelectedIPsAndPorts(ips, ports)
}

case class SelectedPorts(ports: Protocol.HasPort*) extends Selector with Ports {
  override def expressions: Expressions = Expressions.Any
  override def protocols: Protocols = Protocols.port(ports: _*)
  def apply(ports: Protocol.HasPort*): SelectedPorts = SelectedPorts(ports: _*)
}

case class SelectedIPsAndPorts(ips: Seq[Protocol.HasCidr], ports: Seq[Protocol.HasPort]) extends Selector with CIDRs with Ports {
  override def expressions: Expressions = Expressions.Any
  override def protocols: Protocols = Protocols.cidr(ips: _*).merge(Protocols.port(ports: _*))
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
