package com.virtuslab.iat.dsl

trait Identity
object Identity {
  sealed trait Any extends Identity
  case object Any extends Any
}

trait Identities {
  def identities: Set[Identity]
  def merge(other: Identities): Identities = Identities.Some(identities ++ other.identities)
}

object Identities {
  sealed trait Any extends Identities
  case object Any extends Any {
    def identities: Set[Identity] = Set(Identity.Any)
  }
  case class Some(identities: Set[Identity]) extends Identities

  def apply(identities: Set[Identity]): Identities = Some(identities)
  def apply(identities: Identity*): Identities = Some(identities.toSet)
}
