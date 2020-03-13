package com.virtuslab.dsl

trait Resource extends Named
trait Reference extends Labeled
trait ResourceReference extends Resource with Reference

trait Named {
  def name: String // TODO create a proper type
}

trait Label {
  def name: Label.Key
  def value: Label.Value
}

object Label {
  type Key = String // "[prefix/]name" where name is required (same format as value) and prefix is optional (DNS Subdomain Name format)
  type Value = String // 63 characters, [a-z0-9A-Z] with (-_.), basically DNS Label Names with dots and underscores allowed

  sealed trait Expression {
    val key: Key
  }

  sealed trait ExistenceExpression extends Expression

  sealed trait EqualityExpression extends Expression {
    val value: Value
  }

  sealed trait SetExpression extends Expression {
    val values: List[Value]
    def valuesAsString: String = "(" + values.mkString(",") + ")"
  }

  case class ExistsExpression(key: String) extends ExistenceExpression {
    override def toString: String = key
  }

  case class NotExistsExpression(key: String) extends Expression {
    override def toString: String = "!" + key
  }

  case class IsEqualExpression(key: String, value: String) extends EqualityExpression {
    override def toString: String = key + "=" + value
  }

  case class IsNotEqualExpression(key: String, value: String) extends EqualityExpression {
    override def toString: String = key + "!=" + value
  }

  case class InExpression(key: String, values: List[String]) extends SetExpression {
    override def toString: String = key + " in " + valuesAsString
  }
  case class NotInExpression(key: String, values: List[String]) extends SetExpression {
    override def toString: String = key + " notin " + valuesAsString
  }

}

case class NameLabel(value: Label.Value) extends Label {
  override def name: Label.Key = "name"
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
    case labeled: Labeled              => resource.asInstanceOf[Labeled].labels == labeled.labels
    case expressions: LabelExpressions => expressions.matches(resource.asInstanceOf[Labeled].labels)
  }
}

case class ApplicationSelector[S: Selectable](selectable: S) extends Selector[S] {
  def matches[R <: Application with Labeled](resource: R): Boolean = selectable match {
    case labeled: Labeled              => resource.asInstanceOf[Labeled].labels == labeled.labels
    case expressions: LabelExpressions => expressions.matches(resource.asInstanceOf[Labeled].labels)
  }
}

sealed trait EmptySelector extends Selector[Unselected]
case object EmptySelector extends EmptySelector {
  override def selectable: Unselected = Unselected
  def matches[R <: Resource with Labeled](resource: Resource) = false
}

trait LabelExpressions {
  def expressions: Set[Label.Expression]
  def matches(labels: Set[Label]): Boolean
}

object LabelExpressions {
  final case class DefinedLabelExpressions(expressions: Set[Label.Expression]) extends LabelExpressions {
    override def matches(labels: Set[Label]): Boolean = ???
  }

  def apply(expressions: Label.Expression*): LabelExpressions = DefinedLabelExpressions(expressions.toSet)

  // TODO: extract to common place for implicits
  def applicationLabeled(expressions: Label.Expression*): ApplicationSelector[LabelExpressions] =
    ApplicationSelector(LabelExpressions(expressions: _*))
  def namespaceLabeled(expressions: Label.Expression*): NamespaceSelector[LabelExpressions] =
    NamespaceSelector(LabelExpressions(expressions: _*))

  // this DSL enables equality and set based selector expressions analogous to the Kubernetes API
  // The following illustrates mappings from this DSL to k8s selector expressions syntax:
  // "production" -> "production"
  // '"production" doesNotExist ->  "!production"
  // "tier" is "frontend" -> "tier=frontend"
  // "status" isNot "release" -> "status!=release"
  // "env" isIn List("staging", "production") -> "env in (staging,release)"
  // "env" isNotIn List("local", "dev") -> "env notin (local,dev)"
  implicit def stringToExpression(key: String): Label.ExistsExpression = new Label.ExistsExpression(key) {
    def doesNotExist: Label.NotExistsExpression = Label.NotExistsExpression(key)
    def is(value: String): Label.IsEqualExpression = Label.IsEqualExpression(key, value)
    def isNot(value: String): Label.IsNotEqualExpression = Label.IsNotEqualExpression(key, value)
    def isIn(values: List[String]): Label.InExpression = Label.InExpression(key, values)
    def isNotIn(values: List[String]): Label.NotInExpression = Label.NotInExpression(key, values)
  }
}
