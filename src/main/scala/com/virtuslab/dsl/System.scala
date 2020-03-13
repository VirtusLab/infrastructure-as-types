package com.virtuslab.dsl

import com.virtuslab.dsl.Namespace.DefinedNamespace
import com.virtuslab.dsl.System.DefinedSystem

case class SystemBuilder(system: System) {
  private val references: scala.collection.mutable.Set[Reference] = scala.collection.mutable.Set.empty
  private val namespaces: scala.collection.mutable.Set[DefinedNamespace] = scala.collection.mutable.Set.empty

  def references(rs: Reference*): SystemBuilder = {
    references ++= rs
    this
  }

  def namespaces(defined: DefinedNamespace*): SystemBuilder = {
    namespaces ++= defined
    this
  }

  def collect(): Set[DefinedNamespace] = {
    // TODO add validation
    namespaces.toSet
  }

  def build(): DefinedSystem = DefinedSystem(system.name, collect())
}

trait System extends Named

object System {
  final case class SystemReference protected (name: String) extends System {
    def inSystem(f: SystemBuilder => SystemBuilder): DefinedSystem = f(builder).build()
    def builder: SystemBuilder = SystemBuilder(this)
  }

  final case class DefinedSystem protected (name: String, namespaces: Set[DefinedNamespace]) extends System

  def apply(name: String): SystemReference = SystemReference(name)
}
