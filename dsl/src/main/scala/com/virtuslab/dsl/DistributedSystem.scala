package com.virtuslab.dsl

import com.virtuslab.dsl.DistributedSystem.ADistributedSystem
import com.virtuslab.interpreter.{ Context, Interpreter, RootInterpreter }

case class SystemBuilder[Ctx <: Context](system: DistributedSystem) {
  private val refs: scala.collection.mutable.Set[Labeled] = scala.collection.mutable.Set.empty
  private val nss: scala.collection.mutable.Set[Definition[Ctx, DistributedSystem, Namespace, Labeled]] =
    scala.collection.mutable.Set.empty

  def references(rs: Labeled*): SystemBuilder[Ctx] = {
    refs ++= rs
    this
  }

  def namespaces(
      builders: NamespaceBuilder[Ctx]
    )(implicit
      ctx: Ctx,
      ev: Interpreter[Ctx, DistributedSystem, Namespace, Labeled]
    ): SystemBuilder[Ctx] =
    namespaces(builders.build())

  def namespaces(
      namespaces: Definition[Ctx, DistributedSystem, Namespace, Labeled]*
    )(implicit
      ctx: Ctx,
      ev: Interpreter[Ctx, DistributedSystem, Namespace, Labeled]
    ): SystemBuilder[Ctx] = {
    nss ++= namespaces
    this
  }

  // TODO should interpreters take over at least the most of validation?
  def validateState(): Unit = {
    val definitions = nss.toSet
    val members = definitions.flatten((ns: Definition[Ctx, DistributedSystem, Namespace, Labeled]) => ns.members.map(_.obj))
    refs.foreach {
      case nref: Namespace =>
        if (!definitions.exists((ndef: Definition[Ctx, DistributedSystem, Namespace, Labeled]) => nref.labels == ndef.obj.labels)) {
          throw new IllegalStateException("Can't find a namespace definition for reference: " + nref)
        }
      case aref: Application =>
        if (!members.exists {
              case app: Application => aref.labels.equals(app.labels)
              case _                => false
            }) {
          throw new IllegalStateException("Can't find an application definition for reference: " + aref)
        }
      case cref: Connection =>
        if (!members.exists {
              case conn: Connection => cref.labels.equals(conn.labels)
              case _                => false
            }) {
          throw new IllegalStateException("Can't find an connection definition for reference: " + cref)
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

  def collect(): List[Definition[Ctx, DistributedSystem, Namespace, Labeled]] = {
    validateState()
    nss.toList
  }

  def build(
    )(implicit
      ctx: Ctx,
      ev: RootInterpreter[Ctx, DistributedSystem, Namespace]
    ): RootDefinition[Ctx, DistributedSystem, Namespace] =
    Definition(
      ADistributedSystem(system.labels),
      members = collect()
    )

  def name: String = system.name
}

trait DistributedSystem extends Labeled

object DistributedSystem {
  final case class ADistributedSystem[Ctx <: Context] private[dsl] (labels: Labels) extends DistributedSystem {
    def inSystem(
        f: SystemBuilder[Ctx] => SystemBuilder[Ctx]
      )(implicit
        ctx: Ctx,
        ev: RootInterpreter[Ctx, DistributedSystem, Namespace]
      ): RootDefinition[Ctx, DistributedSystem, Namespace] = f(builder).build()
    def builder: SystemBuilder[Ctx] = SystemBuilder(this.asInstanceOf[DistributedSystem])
  }

  def apply[Ctx <: Context](labels: Labels): ADistributedSystem[Ctx] = ADistributedSystem(labels)
  def apply[Ctx <: Context](name: String): ADistributedSystem[Ctx] = ADistributedSystem(Labels(Name(name)))
}
