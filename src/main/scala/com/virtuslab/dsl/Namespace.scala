package com.virtuslab.dsl

import com.virtuslab.dsl.Application.{ ApplicationDefinition, ApplicationReference }
import com.virtuslab.dsl.Namespace.{ NamespaceDefinition, NamespaceReference }

import scala.collection.mutable

trait Namespaced {
  def namespace: Namespace
}

case class NamespaceBuilder(namespace: NamespaceReference, systemBuilder: SystemBuilder) {
  private[this] val connections: mutable.Set[Connection[_, _, _]] = mutable.Set.empty
  private[this] val applications: mutable.Set[ApplicationDefinition] = mutable.Set.empty

  def references(rs: Reference): SystemBuilder = systemBuilder.references(rs)

  def applications(defined: Application*): NamespaceBuilder = {
    systemBuilder.references(defined: _*)
    applications ++= defined.map {
      case a: ApplicationReference  => a.define(this)
      case a: ApplicationDefinition => a
    }
    this
  }

  def connections(defined: Connection[_, _, _]*): NamespaceBuilder = {
    // we don't have a ConnectionReference, so we don't add it to the SystemBuilder
    connections ++= defined
    this
  }

  def collect(): (Set[ApplicationDefinition], Set[Connection[_, _, _]]) = (applications.toSet, connections.toSet)

  def build(): NamespaceDefinition = {
    val (as, cs) = collect()
    val members: Set[Namespaced] = as ++ cs

    val ns = NamespaceDefinition(namespace.labels, members)
    systemBuilder.namespaces(ns)
    ns
  }

  // TODO: extract to common place for implicits
  implicit class ApplicationConnectionOps(app: Application) {
    def communicatesWith(other: Application)(implicit builder: NamespaceBuilder): Connection[_, _, _] = {
      communicatesWith(ApplicationSelector(other))
    }

    def communicatesWith(other: Namespace)(implicit builder: NamespaceBuilder): Connection[_, _, _] = {
      communicatesWith(NamespaceSelector(other))
    }

    def communicatesWith[S <: Selectable](other: Selector[S])(implicit builder: NamespaceBuilder): Connection[_, _, _] = {
      val connection = Connection(
        resourceSelector = ApplicationSelector(app),
        ingress = other,
        egress = ApplicationSelector(app)
      )

      builder.applications(app)
      builder.connections(connection)
      connection
    }
  }
}

trait Namespace extends Reference

object Namespace {
  final case class NamespaceReference protected (labels: Labels) extends Namespace {
    def inNamespace(f: NamespaceBuilder => NamespaceBuilder)(implicit systemBuilder: SystemBuilder): NamespaceDefinition = f(builder).build()
    def builder(implicit systemBuilder: SystemBuilder): NamespaceBuilder = NamespaceBuilder(this, systemBuilder)
  }

  final case class NamespaceDefinition protected (labels: Labels, members: Set[Namespaced]) extends Namespace

  def ref(
      name: String
    )(implicit
      builder: SystemBuilder
    ): NamespaceReference = {
    ref(Labels(Name(name)))
  }

  def ref(
      labels: Labels
    )(implicit
      builder: SystemBuilder
    ): NamespaceReference = {
    val ns = NamespaceReference(labels)
    builder.references(ns)
    ns
  }

  def apply(
      labels: Labels,
      members: Set[Namespaced]
    )(implicit
      builder: SystemBuilder
    ): NamespaceDefinition = {
    ref(labels)(builder).builder.build()
  }
}
