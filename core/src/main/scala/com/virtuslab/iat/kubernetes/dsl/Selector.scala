package com.virtuslab.iat.kubernetes.dsl

import com.virtuslab.iat.dsl._

case class SelectedIPs(
    ips: Seq[Protocol.HasCidr],
    expressions: Expressions,
    identities: Identities)
  extends Peer[SelectedIPs]
  with CIDRs {
  override def protocols: Protocols = Protocols.cidr(ips: _*)

  def withExpressions(expressions: Expressions): SelectedIPs = this.copy(expressions = expressions.merge(this.expressions))
  def withIdentities(identities: Identities): SelectedIPs = this.copy(identities = identities.merge(this.identities))
  def withPorts(ports: Protocol.HasPort*): SelectedIPsAndPorts = SelectedIPsAndPorts(ips, ports, expressions, identities)
}

object SelectedIPs {
  def apply(ips: Protocol.HasCidr*): SelectedIPs = SelectedIPs(ips, Expressions.Any, Identities.Any)
}

case class SelectedPorts(
    ports: Seq[Protocol.HasPort],
    expressions: Expressions,
    identities: Identities)
  extends Peer[SelectedPorts]
  with Ports {
  override def protocols: Protocols = Protocols.port(ports: _*)

  def withExpressions(expressions: Expressions): SelectedPorts = this.copy(expressions = expressions.merge(this.expressions))
  def withIdentities(identities: Identities): SelectedPorts = this.copy(identities = identities.merge(this.identities))
  def withIPs(ips: Protocol.HasCidr*): SelectedIPsAndPorts = SelectedIPsAndPorts(ips, ports, expressions, identities)
}

object SelectedPorts {
  def apply(ports: Protocol.HasPort*): SelectedPorts = SelectedPorts(ports, Expressions.Any, Identities.Any)
}

case class SelectedIPsAndPorts(
    ips: Seq[Protocol.HasCidr],
    ports: Seq[Protocol.HasPort],
    expressions: Expressions,
    identities: Identities)
  extends Peer[SelectedIPsAndPorts]
  with CIDRs
  with Ports {
  override def protocols: Protocols = Protocols.cidr(ips: _*).merge(Protocols.port(ports: _*))
}
