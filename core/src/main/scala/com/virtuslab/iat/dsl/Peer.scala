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
