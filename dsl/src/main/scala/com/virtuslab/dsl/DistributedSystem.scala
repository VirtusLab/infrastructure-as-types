package com.virtuslab.dsl

import com.virtuslab.interpreter.{ Context, Interpreter }

case class SystemBuilder[Ctx <: Context](system: DistributedSystem[Ctx]) {
  private val refs: scala.collection.mutable.Set[Labeled] = scala.collection.mutable.Set.empty
  private val nss: scala.collection.mutable.Set[Definition[_, Namespace]] = scala.collection.mutable.Set.empty

  def references(rs: Labeled*): SystemBuilder[Ctx] = {
    refs ++= rs
    this
  }

  def namespaces(builders: NamespaceBuilder[Ctx])(implicit ctx: Ctx, ev: Interpreter[Ctx, Namespace]): SystemBuilder[Ctx] = namespaces(builders.build())
  def namespaces(namespaces: Namespace*)(implicit ctx: Ctx, ev: Interpreter[Ctx, Namespace]): SystemBuilder[Ctx] = {
    nss ++= namespaces.map(ns => Definition(ns, ns))
    this
  }

  // TODO should interpreters take over at least the most of validation?
  def validateState(): Unit = {
    val definitions: Set[Namespace] = nss.map(d => d.obj).toSet
    val members = definitions.flatten((ns: Namespace) => ns.members)
    refs.foreach {
      case nref: Namespace =>
        if (!definitions.exists((ndef: Namespace) => nref.labels == ndef.labels)) {
          throw new IllegalStateException("Can't find a namespace definition for reference: " + nref)
        }
      case aref: Application =>
        if (!members.exists {
              case Definition(app: Application, _) => aref.labels.equals(app.labels)
              case _                               => false
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

  def collect(): Set[Definition[_, Namespace]] = {
    validateState()
    nss.toSet
  }

  def build(): DistributedSystem[Ctx] = DistributedSystem(system.labels, collect())

  def name: String = system.name
}

final case class DistributedSystem[Ctx <: Context] private[dsl] (labels: Labels, namespaces: Set[Definition[_, Namespace]]) extends Labeled {
  def inSystem(f: SystemBuilder[Ctx] => SystemBuilder[Ctx]): DistributedSystem[Ctx] = f(builder).build()
  def builder: SystemBuilder[Ctx] = SystemBuilder(this)
}

object DistributedSystem {
  def apply[Ctx <: Context](labels: Labels): DistributedSystem[Ctx] = DistributedSystem(labels, Set())
  def apply[Ctx <: Context](name: String): DistributedSystem[Ctx] = DistributedSystem(Labels(Name(name)))
}
