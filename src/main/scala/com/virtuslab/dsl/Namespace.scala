package com.virtuslab.dsl

import com.virtuslab.dsl.Application.{ ApplicationDefinition, ApplicationReference }
import com.virtuslab.dsl.Connection.{ ConnectionDefinition, ConnectionDraft }
import com.virtuslab.dsl.Namespace.{ NamespaceDefinition, NamespaceReference }

import scala.collection.mutable

trait Namespaced {
  def namespace: Namespace
}

trait Definable[T, A <: T, B <: T] { self: A =>
  def define(f: A => B)(implicit builder: NamespaceBuilder): B = f(self)
}

case class NamespaceBuilder(namespace: NamespaceReference, systemBuilder: SystemBuilder) {
  private[this] val connections: mutable.Set[ConnectionDefinition] = mutable.Set.empty
  private[this] val applications: mutable.Set[ApplicationDefinition] = mutable.Set.empty

  def references(rs: Labeled*): SystemBuilder = systemBuilder.references(rs: _*)

  def applications(defined: Application*): NamespaceBuilder = {
    references(defined: _*)
    applications ++= defined.map {
      case a: ApplicationReference  => a.define(this)
      case a: ApplicationDefinition => a
    }
    this
  }

  def connections(defined: Connection*): NamespaceBuilder = {
    val ds = defined.map {
      case c: ConnectionDraft      => c.asDefault(this)
      case c: ConnectionDefinition => c
    }
    references(ds: _*)
    connections ++= ds
    this
  }

  def collect(): (Set[ApplicationDefinition], Set[ConnectionDefinition]) = (applications.toSet, connections.toSet)

  def build(): NamespaceDefinition = {
    val (as, cs) = collect()
    val members: Set[Namespaced] = as ++ cs

    val ns = NamespaceDefinition(namespace.labels, members)
    systemBuilder.namespaces(ns)
    ns
  }

  def name: String = namespace.name

  // TODO: extract to common place for implicits
  implicit class ApplicationConnectionOps(app: Application) {
    def communicatesWith(other: Application): ConnectionDraft = {
      communicatesWith(
        SelectedApplications(
          other,
          Protocols.Any
        )
      )
    }

    def communicatesWith(other: Namespace): ConnectionDraft = {
      communicatesWith(
        SelectedNamespaces(
          other,
          Protocols.Any
        )
      )
    }

    def communicatesWith(other: Selector): ConnectionDraft = {
      ConnectionDraft(
        resourceSelector = SelectedApplications(app, Protocols.Any),
        ingress = other,
        egress = SelectedApplications(app, Protocols.Any)
      )
    }
  }
}

trait Namespace extends Labeled

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
}
