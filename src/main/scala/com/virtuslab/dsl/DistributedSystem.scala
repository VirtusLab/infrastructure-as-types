package com.virtuslab.dsl

import com.virtuslab.dsl.Namespace.DefinedNamespace
import com.virtuslab.dsl.DistributedSystem.DefinedDistributedSystem

case class SystemBuilder(system: DistributedSystem) {
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

  def build(): DefinedDistributedSystem = DefinedDistributedSystem(system.name, collect())
}

trait DistributedSystem extends Named

object DistributedSystem {
  final case class DistributedSystemReference protected (name: String) extends DistributedSystem {
    def inSystem(f: SystemBuilder => SystemBuilder): DefinedDistributedSystem = f(builder).build()
    def builder: SystemBuilder = SystemBuilder(this)
  }

  final case class DefinedDistributedSystem protected (name: String, namespaces: Set[DefinedNamespace]) extends DistributedSystem

  def apply(name: String): DistributedSystemReference = DistributedSystemReference(name)
}
