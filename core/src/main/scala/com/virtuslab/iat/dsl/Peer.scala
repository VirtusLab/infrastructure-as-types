package com.virtuslab.iat.dsl

trait Selection[A] {
  def expressions: Expressions
  def protocols: Protocols
  def identities: Identities
}

trait Peer[A] extends Reference[A] with Selection[A] { self: A =>
}

trait Traffic[A <: Selection[A], B <: Selection[B]] {
  def from: Selection[A]
  def to: Selection[B]
}

object Traffic {
  case class Ingress[A <: Selection[A], B <: Peer[B]](from: Selection[A], to: Peer[B])
    extends Traffic[A, B]
    with Patchable[Ingress[A, B]]

  case class Egress[A <: Peer[A], B <: Selection[B]](from: Peer[A], to: Selection[B])
    extends Traffic[A, B]
    with Patchable[Egress[A, B]]
}

object Peer {
  import com.virtuslab.iat.dsl.Expressions.Expression

  final case class Selected[A](
      expressions: Expressions = Expressions.Any,
      protocols: Protocols = Protocols.Any,
      identities: Identities = Identities.Any)
    extends Selection[A]
    with Patchable[Selected[A]] {
    def withExpressions(expressions: Expressions): Selected[A] = this.copy(expressions = this.expressions.merge(expressions))
    def withExpressions(expressions: Expression*): Selected[A] =
      this.copy(expressions = this.expressions.merge(Expressions(expressions: _*)))
    def withProtocols(protocols: Protocols): Selected[A] = this.copy(protocols = this.protocols.merge(protocols))
    def withIdentities(identities: Identities): Selected[A] = this.copy(identities = this.identities.merge(identities))
    def withPorts(ports: Protocol.HasPort*): Selected[A] = this.copy(protocols = this.protocols.merge(Protocols.ports(ports: _*)))
    def withIPs(ips: Protocol.HasCidr*): Selected[A] = this.copy(protocols = this.protocols.merge(Protocols.cidrs(ips: _*)))
  }
  object Selected {
    final case class Any(
        expressions: Expressions = Expressions.Any,
        protocols: Protocols = Protocols.Any,
        identities: Identities = Identities.Any)
      extends Selection[scala.Any] {
      def withType[T]: Selected[T] = Selected[T](expressions, protocols, identities)
      def withExpressions(expressions: Expressions): Selected.Any = this.copy(expressions = this.expressions.merge(expressions))
      def withExpressions(expressions: Expression*): Selected.Any =
        this.copy(expressions = this.expressions.merge(Expressions(expressions: _*)))
      def withProtocols(protocols: Protocols): Selected.Any = this.copy(protocols = this.protocols.merge(protocols))
      def withIdentities(identities: Identities): Selected.Any = this.copy(identities = this.identities.merge(identities))
      def withPorts(ports: Protocol.HasPort*): Selected.Any =
        this.copy(protocols = this.protocols.merge(Protocols.ports(ports: _*)))
      def withIPs(ips: Protocol.HasCidr*): Selected.Any = this.copy(protocols = this.protocols.merge(Protocols.cidrs(ips: _*)))
    }

    final case class None(
        expressions: Expressions = Expressions.Any,
        protocols: Protocols = Protocols.Any,
        identities: Identities = Identities.Any)
      extends Selection[scala.Nothing] {
      def withType[T]: Selected[T] = Selected[T](expressions, protocols, identities)
      def withExpressions(expressions: Expressions): Selected.None = this.copy(expressions = this.expressions.merge(expressions))
      def withExpressions(expressions: Expression*): Selected.None =
        this.copy(expressions = this.expressions.merge(Expressions(expressions: _*)))
      def withProtocols(protocols: Protocols): Selected.None = this.copy(protocols = this.protocols.merge(protocols))
      def withIdentities(identities: Identities): Selected.None = this.copy(identities = this.identities.merge(identities))
      def withPorts(ports: Protocol.HasPort*): Selected.None =
        this.copy(protocols = this.protocols.merge(Protocols.ports(ports: _*)))
      def withIPs(ips: Protocol.HasCidr*): Selected.None = this.copy(protocols = this.protocols.merge(Protocols.cidrs(ips: _*)))
    }
  }

  def any: Selected.Any = Selected.Any()
  def none: Selected.None = Selected.None()
  def withType[T]: Selected[T] = any.withType[T]
}
