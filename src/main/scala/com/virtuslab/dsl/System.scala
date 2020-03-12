package com.virtuslab.dsl

import com.virtuslab.dsl.Namespace.DefinedNamespace

case class SystemBuilder(system: System) {
  private val namespaces: scala.collection.mutable.Set[DefinedNamespace] = scala.collection.mutable.Set.empty

  def Namespaces(defined: DefinedNamespace*): SystemBuilder = {
    namespaces ++= defined
    this
  }

  def collect(): Set[DefinedNamespace] = namespaces.toSet
}

trait System extends Named

object System {
  case class SystemReference protected (name: String) extends System {

    def inSystem(f: SystemBuilder => SystemBuilder): DefinedSystem = {
      val builder = f(SystemBuilder(this))
      val members = builder.collect()
      DefinedSystem(name, members)
    }
  }

  final case class DefinedSystem protected (name: String, namespaces: Set[DefinedNamespace]) extends System

  def apply(name: String): SystemReference = {
    SystemReference(name)
  }
}
