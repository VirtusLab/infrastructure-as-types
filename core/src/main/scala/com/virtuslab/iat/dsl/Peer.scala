package com.virtuslab.iat.dsl

trait Peer[A] { self: A =>
  def expressions: Expressions
  def protocols: Protocols
  def identities: Identities
  def transform[B <: Peer[B]](f: Peer[A] => Peer[B]): Peer[B] = f(this)
}

object Peer {
  trait Type[A <: Peer[A]]
  object Type {
    case object Any extends Type[Peer.Any]
    case class AType[A <: Peer[A]](r: A) extends Type[A]
  }
  sealed trait Any extends Peer[Peer.Any]
  case object Any extends Any {
    override def expressions: Expressions = Expressions.Any
    override def protocols: Protocols = Protocols.Any
    override def identities: Identities = Identities.Any
  }
  case class Selected(
      expressions: Expressions,
      protocols: Protocols,
      identities: Identities)
    extends Peer[Selected]
}

trait Traffic[A <: Peer[A], B <: Peer[B]] {
  def from: A
  def to: B
}
object Traffic {
  case class Ingress[A <: Peer[A], B <: Peer[B]](from: A, to: B) extends Traffic[A, B]
  case class Egress[A <: Peer[A], B <: Peer[B]](to: B, from: A) extends Traffic[A, B]
}
