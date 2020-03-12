package com.virtuslab.dsl

import cats.data.NonEmptyList
import com.virtuslab.dsl.Namespace.NamespaceReference

trait Resource extends Named {}

trait Named {
  def name: String // TODO create a proper type
}

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
      DefinedNamespace(name, labels, ???)
    }
  }

  final case class DefinedNamespace protected (
      name: String,
      labels: Set[Label],
      members: NonEmptyList[Namespaced])
    extends Namespace
    with Labeled

  def apply(name: String): NamespaceReference = {
    NamespaceReference(name, Set(NameLabel(name)))
  }
}

trait Label {
  type Key = String // "[prefix/]name" where name is required (same format as value) and prefix is optional (DNS Subdomain Name format)
  type Value = String // 63 characters, [a-z0-9A-Z] with (-_.), basically DNS Label Names with dots and underscores allowed

  def name: Key
  def value: Value
}

trait Labeled {
  def labels: Set[Label]
}

object Labels {
  def apply(values: Label*): Labeled = apply(values.toSet)
  def apply(values: Set[Label]): Labeled = new Labeled {
    override def labels: Set[Label] = values
  }
}

sealed trait Unselected
case object Unselected extends Unselected

final case class LabelExpression(expression: String) // TODO a proper expression DSL with =, ==, and != and sets
trait LabelExpressions {
  def expressions: Set[LabelExpression]
}

case class NameLabel(value: Label#Value) extends Label {
  override def name: Key = "name"
}

class Selectable[-T]
object Selectable {
  implicit object UnselectedWitness extends Selectable[Unselected]
  implicit object LabelWitness extends Selectable[Labeled] // k8s API: "matchLabels"
  implicit object LabelExpressionsWitness extends Selectable[LabelExpressions] // k8s API: "matchExpressions"
}

abstract class PlaceholderSelector[-A, S >: A: Selectable]
abstract class Selector[A: Selectable] extends PlaceholderSelector[A, A] {
  def selectable: A
}

case class NamespaceSelector[S: Selectable](selectable: S) extends Selector[S] {
  def matches[R <: Namespace with Labeled](resource: R): Boolean = selectable match {
    case _: Labeled          => resource.asInstanceOf[Labeled] == selectable
    case _: LabelExpressions => ???
  }
}

case class ApplicationSelector[S: Selectable](selectable: S) extends Selector[S] {
  def matches[R <: Application with Labeled](resource: R): Boolean = selectable match {
    case _: Labeled          => resource.asInstanceOf[Labeled] == selectable
    case _: LabelExpressions => ???
  }
}

sealed trait EmptySelector extends Selector[Unselected]
case object EmptySelector extends EmptySelector {
  override def selectable: Unselected = Unselected
  def matches[R <: Resource with Labeled](resource: Resource) = false
}
