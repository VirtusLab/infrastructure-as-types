package com.virtuslab.dsl

import com.virtuslab.dsl.Namespace.DefinedNamespace
import com.virtuslab.dsl.SystemDef.DefinedSystemDef

case class SystemBuilder(system: SystemDef) {
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

  def build(): DefinedSystemDef = DefinedSystemDef(system.name, collect())
}

trait SystemDef extends Named

object SystemDef {
  final case class SystemDefReference protected (name: String) extends SystemDef {
    def inSystem(f: SystemBuilder => SystemBuilder): DefinedSystemDef = f(builder).build()
    def builder: SystemBuilder = SystemBuilder(this)
  }

  final case class DefinedSystemDef protected (name: String, namespaces: Set[DefinedNamespace]) extends SystemDef

  def apply(name: String): SystemDefReference = SystemDefReference(name)
}
