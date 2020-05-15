package com.virtuslab.iat.dsl

trait Peer[A] extends Reference[A] { self: A =>
  def expressions: Expressions
  def protocols: Protocols
  def identities: Identities
}

object Peer {
  sealed trait Any extends Peer[Any]
  case object Any extends Any {
    override def expressions: Expressions = Expressions.Any
    override def protocols: Protocols = Protocols.Any
    override def identities: Identities = Identities.Any
  }
  sealed trait None extends Peer[None]
  case object None extends None {
    override def expressions: Expressions = Expressions.None
    override def protocols: Protocols = Protocols.None
    override def identities: Identities = Identities.None
  }

  import scala.reflect.runtime.universe.TypeTag
  import com.virtuslab.iat.internal.TypeTagged

  final case class Selected[A: TypeTag](
      expressions: Expressions,
      protocols: Protocols,
      identities: Identities)
    extends TypeTagged[Selected[A]]
    with Peer[Selected[A]]
    with Patchable[Selected[A]]
    with Transformable[Selected[A]]
}

trait Traffic[A <: Peer[A], B <: Peer[B]] {
  def from: Peer[A]
  def to: Peer[B]
}

object Traffic {
  case class Ingress[A <: Peer[A], B <: Peer[B]](from: Peer[A], to: Peer[B])
    extends Traffic[A, B]
    with Patchable[Ingress[A, B]]
    with Transformable[Ingress[A, B]]

  case class Egress[A <: Peer[A], B <: Peer[B]](from: Peer[A], to: Peer[B])
    extends Traffic[A, B]
    with Patchable[Egress[A, B]]
    with Transformable[Egress[A, B]]
}
