package com.virtuslab.dsl

import com.virtuslab.dsl.Application.{ ApplicationDefinition, ApplicationReference }
import com.virtuslab.dsl.DistributedSystem.DefinedDistributedSystem
import com.virtuslab.dsl.Namespace.{ NamespaceDefinition, NamespaceReference }

case class SystemBuilder(system: DistributedSystem) {
  private val refs: scala.collection.mutable.Set[Labeled] = scala.collection.mutable.Set.empty
  private val nss: scala.collection.mutable.Set[NamespaceDefinition] = scala.collection.mutable.Set.empty

  def references(rs: Labeled*): SystemBuilder = {
    refs ++= rs
    this
  }

  def namespaces(defined: NamespaceDefinition*): SystemBuilder = {
    nss ++= defined
    this
  }

  def validateState(): Unit = {
    val ms = nss.flatten((ns: NamespaceDefinition) => ns.members)
    println(s"[${system.name}] ms: $ms")
    refs.foreach {
      case nref: NamespaceReference =>
        if (!nss.exists((ndef: NamespaceDefinition) => nref.labels == ndef.labels)) {
          throw new IllegalStateException("Can't find a namespace definition for reference: " + nref)
        }
      case aref: ApplicationReference =>
        if (!ms.exists {
              case adef: ApplicationDefinition => aref.labels.equals(adef.labels)
              case _                           => false
            }) {
          throw new IllegalStateException("Can't find an application definition for reference: " + aref)
        }
      case r => println("Unsupported reference validation: " + r)
      // TODO add Configuration ref check
      // TODO add Connections ref/selector checks
      // TODO add
    }
    if (nss.isEmpty) {
      println(s"The system '${system.name}' has no namespaces")
    }
  }

  def collect(): Set[NamespaceDefinition] = {
    validateState()
    nss.toSet
  }

  def build(): DefinedDistributedSystem = DefinedDistributedSystem(system.name, collect())
}

trait DistributedSystem extends Named

object DistributedSystem {

  final case class DistributedSystemReference protected (name: String) extends DistributedSystem {
    def inSystem(f: SystemBuilder => SystemBuilder): DefinedDistributedSystem = f(builder).build()

    def builder: SystemBuilder = SystemBuilder(this)
  }

  final case class DefinedDistributedSystem protected (name: String, namespaces: Set[NamespaceDefinition]) extends DistributedSystem

  def apply(name: String): DistributedSystemReference = DistributedSystemReference(name)
}
