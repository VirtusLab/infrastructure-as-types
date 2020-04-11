package com.virtuslab.dsl

import com.virtuslab.dsl.Namespace.NamespaceDefinition
import com.virtuslab.interpreter.{ Context, Interpreter }

import scala.collection.mutable

case class NamespaceBuilder[Ctx <: Context](namespace: Namespace, systemBuilder: SystemBuilder[Ctx]) {
  private[this] val connections: mutable.Set[Definition[Ctx, Connection]] = mutable.Set.empty
  private[this] val applications: mutable.Set[Definition[Ctx, Application]] = mutable.Set.empty

  def references(rs: Labeled*): SystemBuilder[Ctx] = systemBuilder.references(rs: _*)

  def applications(apps: Application*)(implicit ctx: Ctx, ev: Interpreter[Ctx, Application]): NamespaceBuilder[Ctx] = {
    references(apps: _*)
    applications ++= apps.map(Definition(_)(ctx, ev, this))
    this
  }

  def connections(conns: Connection*)(implicit ctx: Ctx, ev: Interpreter[Ctx, Connection]): NamespaceBuilder[Ctx] = {
    references(conns: _*)
    val ds = conns.map(Definition(_)(ctx, ev, this))
    connections ++= ds
    this
  }

  def collect(): (Set[Definition[Ctx, Application]], Set[Definition[Ctx, Connection]]) = (applications.toSet, connections.toSet)

  def build()(implicit ctx: Ctx, ev: Interpreter[Ctx, Namespace]): NamespaceDefinition[Ctx] = {
    val (as, cs) = collect()
    val members: Set[Definition[Ctx, Any]] = (as ++ cs).asInstanceOf[Set[Definition[Ctx, Any]]]
    val ns = NamespaceDefinition(namespace.labels, members)
    systemBuilder.namespaceDefinitions(Seq(ns))
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

trait Namespace extends Labeled

object Namespace {
  final case class NamespaceReference private[dsl] (labels: Labels) extends Labeled with Namespace {
    def inNamespace[Ctx <: Context](
        f: NamespaceBuilder[Ctx] => NamespaceBuilder[Ctx]
      )(implicit
        systemBuilder: SystemBuilder[Ctx],
        ctx: Ctx,
        ev: Interpreter[Ctx, Namespace]
      ): NamespaceDefinition[Ctx] = f(builder).build()

    def builder[Ctx <: Context](implicit systemBuilder: SystemBuilder[Ctx]): NamespaceBuilder[Ctx] =
      NamespaceBuilder[Ctx](this, systemBuilder)
  }

  final case class NamespaceDefinition[Ctx <: Context] private[dsl] (
      labels: Labels,
      members: Set[Definition[Ctx, Any]]
    )(implicit
      ctx: Ctx,
      ev: Interpreter[Ctx, Namespace])
    extends Namespace
    with Definition[Ctx, Namespace] {
    override def obj: Namespace = this
    override def namespace: Namespace = this
    override def interpret(): Iterable[Ctx#Ret] = ev(this)
  }

  def apply[Ctx <: Context](name: String)(implicit builder: SystemBuilder[Ctx], ctx: Ctx): NamespaceReference = {
    apply(Labels(Name(name)))
  }

  def apply[Ctx <: Context](labels: Labels)(implicit builder: SystemBuilder[Ctx], ctx: Ctx): NamespaceReference = {
    val ns = NamespaceReference(labels)
    builder.references(ns)
    ns
  }
}
