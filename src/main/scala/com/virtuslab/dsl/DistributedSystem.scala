package com.virtuslab.dsl

import com.virtuslab.dsl.Application.{ ApplicationDefinition, ApplicationReference }
import com.virtuslab.dsl.DistributedSystem.DistributedSystemDefinition
import com.virtuslab.dsl.Namespace.{ NamespaceDefinition, NamespaceReference }

case class SystemBuilder(system: DistributedSystem) {
  private val refs: scala.collection.mutable.Set[Labeled] = scala.collection.mutable.Set.empty
  private val nss: scala.collection.mutable.Set[NamespaceDefinition] = scala.collection.mutable.Set.empty

  def references(rs: Labeled*): SystemBuilder = {
    refs ++= rs
    this
  }

  def namespaces(builders: NamespaceBuilder): SystemBuilder = namespaces(builders.build())
  def namespaces(ref: NamespaceReference): SystemBuilder = namespaces(ref.builder(this))
  def namespaces(defined: NamespaceDefinition*): SystemBuilder = {
    nss ++= defined
    this
  }

  // TODO should interpreters take over at least the most of validation?
  def validateState(): Unit = {
    val ms = nss.flatten((ns: NamespaceDefinition) => ns.members)
//    println(s"[${system.name}] ms: $ms")
    refs.foreach {
      case nref: NamespaceReference =>
        if (!nss.exists((ndef: NamespaceDefinition) => nref.labels == ndef.labels)) {
          throw new IllegalStateException("Can't find a namespace definition for reference: " + nref)
        }
      case aref: ApplicationReference =>
        if (!ms.exists {
              case Definition(adef: ApplicationDefinition, _) => aref.labels.equals(adef.labels)
              case _                                          => false
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

  def build(): DistributedSystemDefinition = DistributedSystemDefinition(system.labels, collect())

  def name: String = system.name
}

trait DistributedSystem extends Labeled

object DistributedSystem {

  final case class DistributedSystemReference protected (labels: Labels) extends DistributedSystem {
    def inSystem(f: SystemBuilder => SystemBuilder): DistributedSystemDefinition = f(builder).build()
    def builder: SystemBuilder = SystemBuilder(this)
  }

  final case class DistributedSystemDefinition protected (labels: Labels, namespaces: Set[NamespaceDefinition]) extends DistributedSystem

  def ref(labels: Labels): DistributedSystemReference = DistributedSystemReference(labels)
  def ref(name: String): DistributedSystemReference = DistributedSystemReference(Labels(Name(name)))
}
