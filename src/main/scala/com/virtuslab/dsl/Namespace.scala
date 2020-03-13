package com.virtuslab.dsl

import com.virtuslab.dsl.Application.{ ApplicationReference, DefinedApplication }
import com.virtuslab.dsl.Namespace.NamespaceReference

trait Namespaced {
  def namespace: Namespace
}

case class NamespaceBuilder(namespace: Namespace) {
  private val connections: scala.collection.mutable.Set[Connection[_, _, _]] = scala.collection.mutable.Set.empty
  private val applications: scala.collection.mutable.Set[Application] = scala.collection.mutable.Set.empty

  def Applications(defined: Application*): NamespaceBuilder = {
    applications ++= defined
    this
  }

  def Connections(defined: Connection[_, _, _]*): NamespaceBuilder = {
    connections ++= defined
    this
  }

  def collect(): (Set[Application], Set[Connection[_, _, _]]) = (applications.toSet, connections.toSet)

  //TODO: extract to common place for implicits
  implicit class ApplicationConnectionOps(app: Application) {
    def communicatesWith(other: Application)(implicit ns: Namespace): Connection[_, _, _] = {
      val connection = Connection(
        resourceSelector = ApplicationSelector(app),
        ingress = ApplicationSelector(other),
        egress = ApplicationSelector(app)
      )
      connections += connection

      connection
    }

    def communicatesWith(namespace: NamespaceReference)(implicit ns: Namespace): Connection[_, _, _] = {
      val connection = Connection(
        resourceSelector = ApplicationSelector(app),
        ingress = NamespaceSelector(namespace),
        egress = ApplicationSelector(app)
      )
      connections += connection

      connection
    }
  }

  implicit def nsBuilderToNs(implicit builder: NamespaceBuilder): Namespace = {
    builder.namespace
  }

}

trait Namespace extends Resource with Labeled

object Namespace {
  final case class NamespaceReference protected (name: String, labels: Set[Label]) extends Namespace with Labeled {
    def labeled(ls: Label*): NamespaceReference = {
      NamespaceReference(name, labels ++ ls)
    }

    def inNamespace(f: NamespaceBuilder => NamespaceBuilder): DefinedNamespace = {
      val builder = f(NamespaceBuilder(this))
      val (apps, conns) = builder.collect()
      val members: Set[Namespaced] = apps.map {
        case a: ApplicationReference => a.bind()(builder.namespace)
        case a: DefinedApplication   => a
      } ++ conns

      DefinedNamespace(name, labels, members)
    }
  }

  final case class DefinedNamespace protected (
      name: String,
      labels: Set[Label],
      members: Set[Namespaced])
    extends Namespace
    with Labeled

  def apply(name: String): NamespaceReference = {
    NamespaceReference(name, Set(NameLabel(name)))
  }
}
