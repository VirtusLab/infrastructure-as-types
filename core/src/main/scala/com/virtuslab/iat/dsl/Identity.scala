package com.virtuslab.iat.dsl

trait Identity
object Identity {
  sealed trait Any extends Identity
  case object Any extends Any
  case class DNS(host: String) extends Identity
}

trait Identities {
  def identities: Set[Identity]
}
object Identities {
  sealed trait Any extends Identities
  case object Any extends Any {
    def identities: Set[Identity] = Set()
  }
  case class Some(identities: Set[Identity]) extends Identities
}
