package com.virtuslab.dsl

import com.virtuslab.dsl.Definition.ADefinition
import com.virtuslab.dsl.Namespace.{ NamespaceDefinition, NamespaceReference }
import com.virtuslab.interpreter.{ Context, Interpreter }

case class SystemBuilder[Ctx <: Context](system: DistributedSystem[Ctx]) {
  private val refs: scala.collection.mutable.Set[Labeled] = scala.collection.mutable.Set.empty
  private val nss: scala.collection.mutable.Set[NamespaceDefinition[Ctx]] = scala.collection.mutable.Set.empty

  def references(rs: Labeled*): SystemBuilder[Ctx] = {
    refs ++= rs
    this
  }

  def namespaces(ns: Namespace*)(implicit ctx: Ctx, ev: Interpreter[Ctx, Namespace]): SystemBuilder[Ctx] =
    namespaceDefinitions(ns.map {
      case ns: NamespaceReference       => ns.builder(this).build()
      case ns: NamespaceDefinition[Ctx] => ns
    })

  def namespaces(builders: NamespaceBuilder[Ctx])(implicit ctx: Ctx, ev: Interpreter[Ctx, Namespace]): SystemBuilder[Ctx] =
    namespaceDefinitions(Seq(builders.build()))

  private[dsl] def namespaceDefinitions(namespaces: Seq[NamespaceDefinition[Ctx]])(implicit ctx: Ctx, ev: Interpreter[Ctx, Namespace]): SystemBuilder[Ctx] = {
    nss ++= namespaces
    this
  }

  // TODO should interpreters take over at least the most of validation?
  def validateState(): Unit = {
    val definitions: Set[NamespaceDefinition[Ctx]] = nss.toSet
    val members = definitions.flatten((ns: NamespaceDefinition[Ctx]) => ns.members)
    refs.foreach {
      case nref: Namespace =>
        if (!definitions.exists((ndef: NamespaceDefinition[Ctx]) => nref.labels == ndef.labels)) {
          throw new IllegalStateException("Can't find a namespace definition for reference: " + nref)
        }
      case aref: Application =>
        if (!members.exists {
              case ADefinition(app: Application, _) => aref.labels.equals(app.labels)
              case _                                => false
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

  def collect(): Set[NamespaceDefinition[Ctx]] = {
    validateState()
    nss.toSet
  }

  def build(): DistributedSystem[Ctx] = DistributedSystem(system.labels, collect())

  def name: String = system.name
}

final case class DistributedSystem[Ctx <: Context] private[dsl] (labels: Labels, namespaces: Set[NamespaceDefinition[Ctx]]) extends Labeled {
  def inSystem(f: SystemBuilder[Ctx] => SystemBuilder[Ctx]): DistributedSystem[Ctx] = f(builder).build()
  def builder: SystemBuilder[Ctx] = SystemBuilder(this)
}

object DistributedSystem {
  def apply[Ctx <: Context](labels: Labels): DistributedSystem[Ctx] = DistributedSystem(labels, Set())
  def apply[Ctx <: Context](name: String): DistributedSystem[Ctx] = DistributedSystem(Labels(Name(name)))
}
