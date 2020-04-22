package com.virtuslab.dsl

import com.virtuslab.interpreter.{ Context, Interpreter }

import scala.collection.mutable

case class NamespaceBuilder[Ctx <: Context](namespace: Namespace, systemBuilder: SystemBuilder[Ctx]) {
  private[this] val connections: mutable.Set[Definition[Ctx, Namespace, Connection, Labeled]] = mutable.Set.empty
  private[this] val applications: mutable.Set[Definition[Ctx, Namespace, Application, Labeled]] = mutable.Set.empty

  def references(rs: Labeled*): SystemBuilder[Ctx] = systemBuilder.references(rs: _*)

  def applications(
      apps: Application*
    )(implicit
      ctx: Ctx,
      ev: Interpreter[Ctx, Namespace, Application, Labeled]
    ): NamespaceBuilder[Ctx] = {
    references(apps: _*)
    applications ++= apps.map(Definition(_)(ctx, ev, this))
    this
  }

  def connections(
      conns: Connection*
    )(implicit
      ctx: Ctx,
      ev: Interpreter[Ctx, Namespace, Connection, Labeled]
    ): NamespaceBuilder[Ctx] = {
    references(conns: _*)
    val ds = conns.map(Definition(_)(ctx, ev, this))
    connections ++= ds
    this
  }

  def collect(): (List[Definition[Ctx, Namespace, Application, Labeled]], List[Definition[Ctx, Namespace, Connection, Labeled]]) =
    (applications.toList, connections.toList)

  def build(
    )(implicit
      ctx: Ctx,
      ev: Interpreter[Ctx, DistributedSystem, Namespace, Labeled]
    ): Definition[Ctx, DistributedSystem, Namespace, Labeled] = {
    val (as, cs) = collect()
    val members: List[Definition[Ctx, Namespace, Labeled, Labeled]] =
      (as ++ cs).asInstanceOf[List[Definition[Ctx, Namespace, Labeled, Labeled]]]
    val ns: Definition[Ctx, DistributedSystem, Namespace, Labeled] = Definition(this.systemBuilder.system, namespace, members)
    systemBuilder.namespaces(ns)
    ns
  }

  def name: String = namespace.name

  // TODO: extract to common place for implicits
  implicit class ApplicationConnectionOps(app: Application) {
    def communicatesWith(other: Application): Connection = {
      communicatesWith(
        SelectedApplications(
          other,
          Protocols.Any
        )
      )
    }

    def communicatesWith(other: Namespace): Connection = {
      communicatesWith(
        SelectedNamespaces(
          other,
          Protocols.Any
        )
      )
    }

    def communicatesWith(other: Selector): Connection = {
      Connection(
        resourceSelector = SelectedApplications(app, Protocols.Any),
        ingress = other,
        egress = SelectedApplications(app, Protocols.Any)
      )
    }
  }
}

trait Namespace extends Labeled {
  def name: String = labels.name.value
}

object Namespace {
  final case class ANamespace[Ctx <: Context] private[dsl] (labels: Labels) extends Namespace {
    def inNamespace(
        f: NamespaceBuilder[Ctx] => NamespaceBuilder[Ctx]
      )(implicit
        ctx: Ctx,
        ev: Interpreter[Ctx, DistributedSystem, Namespace, Labeled],
        systemBuilder: SystemBuilder[Ctx]
      ): Definition[Ctx, DistributedSystem, Namespace, Labeled] = f(builder).build()

    def builder(implicit systemBuilder: SystemBuilder[Ctx]): NamespaceBuilder[Ctx] =
      NamespaceBuilder[Ctx](this, systemBuilder)

  }

  def apply[Ctx <: Context](name: String)(implicit builder: SystemBuilder[Ctx], ctx: Ctx): ANamespace[Ctx] = {
    apply(Labels(Name(name)))
  }

  def apply[Ctx <: Context](labels: Labels)(implicit builder: SystemBuilder[Ctx], ctx: Ctx): ANamespace[Ctx] = {
    val ns = ANamespace[Ctx](labels)
    builder.references(ns)
    ns
  }
}
