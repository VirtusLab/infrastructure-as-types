package com.virtuslab.dsl

trait Resource extends Named {}

trait ResourceReference extends Resource with Labeled

trait Named {
  def name: String // TODO create a proper type
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
