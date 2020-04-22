package com.virtuslab.dsl

import com.virtuslab.dsl.Expressions.Expression

trait Expressions extends HasShortDescription {
  def expressions: Set[Expression]
}

object Expressions {
  def apply(expressions: Expression*): Expressions = Some(expressions.toSet)
  def apply(expressions: Set[Expression]): Expressions = Some(expressions)

  sealed trait Any extends Expressions
  case object Any extends Any {
    override def expressions: Set[Expression] = Set()
    override def asShortString: String = "any"
  }
  case class Some(expressions: Set[Expression]) extends Expressions {
    override def toString: String = expressions.mkString(",")
    override def asShortString: String = toString.replaceAll("[!@#$%^&*()+=<>|/\\\\_,.]*", "-").take(20)
  }

  sealed trait Expression {
    val key: String
  }

  sealed trait EqualityExpression extends Expression {
    val value: String
  }

  sealed trait SetExpression extends Expression {
    val values: Seq[String]
    def valuesAsString: String = "(" + values.mkString(",") + ")"
  }

  case class ExistsExpression(key: String) extends Expression {
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

  case class InExpression(key: String, values: Seq[String]) extends SetExpression {
    override def toString: String = key + " in " + valuesAsString
  }

  case class NotInExpression(key: String, values: Seq[String]) extends SetExpression {
    override def toString: String = key + " notin " + valuesAsString
  }

  import scala.language.implicitConversions

  //noinspection TypeAnnotation
  // this DSL enables equality and set based selector expressions analogous to the Kubernetes API
  // The following illustrates mappings from this DSL to k8s selector expressions syntax:
  // "production" -> "production"
  // '"production" doesNotExist ->  "!production"
  // "tier" is "frontend" -> "tier=frontend"
  // "status" isNot "release" -> "status!=release"
  // "env" isIn List("staging", "production") -> "env in (staging,release)"
  // "env" isNotIn List("local", "dev") -> "env notin (local,dev)"
  implicit def stringToExpression(key: String) = new ExistsExpression(key) {
    def doesNotExist: NotExistsExpression = NotExistsExpression(key)
    def is(value: String): IsEqualExpression = IsEqualExpression(key, value)
    def isNot(value: String): IsNotEqualExpression = IsNotEqualExpression(key, value)
    def in(values: String*): InExpression = InExpression(key, values)
    def isNotIn(values: List[String]): NotInExpression = NotInExpression(key, values)
  }

  def applicationLabeled(expressions: Expression*): ApplicationSelector =
    SelectedApplications(
      expressions = Expressions(expressions: _*),
      protocols = Protocols.Any
    )

  def namespaceLabeled(expressions: Expression*): NamespaceSelector =
    SelectedNamespaces(
      expressions = Expressions(expressions: _*),
      protocols = Protocols.Any
    )
}

trait Label extends Expressions.EqualityExpression {
  override def toString: String = key + "=" + value
}

final case class Name(value: String) extends Label {
  val key: String = "name"
}

final case class UntypedLabel(key: String, value: String) extends Label

case class Labels(name: Name, private val values: Set[Label]) extends Expressions {

  def all: Set[Label] = Set(name) ++ values
  def tail: Set[Label] = values
  def toMap: Map[String, String] = all.map(l => l.key -> l.value).toMap
  def map[B](f: Label => B): Set[B] = all.map(f)

  override def expressions: Set[Expression] = all.map(l => l)
  override def asShortString: String = name.value.take(20)
}

object Labels {
  def apply(name: Name, values: Label*): Labels = Labels(name, values.toSet)
}
