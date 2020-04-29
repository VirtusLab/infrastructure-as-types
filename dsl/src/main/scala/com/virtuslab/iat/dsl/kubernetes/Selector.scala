package com.virtuslab.iat.dsl.kubernetes

import com.virtuslab.iat.dsl._

trait Selector {
  def expressions: Expressions
  def protocols: Protocols
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

case class SelectedPorts(ports: Seq[Protocol.HasPort]) extends Selector with Ports {
  override def expressions: Expressions = Expressions.Any
  override def protocols: Protocols = Protocols.port(ports: _*)
  def apply(ports: Protocol.HasPort*): SelectedPorts = SelectedPorts(ports)
}

case class SelectedIPsAndPorts(ips: Seq[Protocol.HasCidr], ports: Seq[Protocol.HasPort]) extends Selector with CIDRs with Ports {
  override def expressions: Expressions = Expressions.Any
  override def protocols: Protocols = Protocols.cidr(ips: _*).merge(Protocols.port(ports: _*))
}
