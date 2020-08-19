package com.virtuslab.iat.dsl

import com.virtuslab.iat.dsl.Expressions.{ ExistsExpression, Expression, InExpression, IsEqualExpression, IsNotEqualExpression, NotExistsExpression, NotInExpression }

trait Expressions {
  def expressions: Set[Expression]
  def merge(other: Expressions): Expressions = Expressions(expressions ++ other.expressions)
}

object Expressions {
  def apply(expressions: Expression*): Expressions = Some(expressions.toSet)
  def apply(expressions: Set[Expression]): Expressions = Some(expressions)

  sealed trait Any extends Expressions
  case object Any extends Any {
    override def expressions: Set[Expression] = Set()
  }
  sealed trait None extends Expressions
  case object None extends None {
    override def expressions: Set[Expression] = Set()
  }
  case class Some(expressions: Set[Expression]) extends Expressions {
    override def toString: String = expressions.mkString(",")
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

  sealed case class ExistsExpression(key: String) extends Expression {
    override def toString: String = key
  }

  final case class NotExistsExpression(key: String) extends Expression {
    override def toString: String = "!" + key
  }

  final case class IsEqualExpression(key: String, value: String) extends EqualityExpression {
    override def toString: String = key + "=" + value
  }

  final case class IsNotEqualExpression(key: String, value: String) extends EqualityExpression {
    override def toString: String = key + "!=" + value
  }

  final case class InExpression(key: String, values: Seq[String]) extends SetExpression {
    override def toString: String = key + " in " + valuesAsString
  }

  final case class NotInExpression(key: String, values: Seq[String]) extends SetExpression {
    override def toString: String = key + " notin " + valuesAsString
  }

  object ops extends ExpressionsOps
}

trait ExpressionsOps {
  import scala.language.implicitConversions

  // this DSL enables equality and set based selector expressions analogous to the Kubernetes API
  // The following illustrates mappings from this DSL to k8s selector expressions syntax:
  // "production" -> "production"
  // '"production" doesNotExist ->  "!production"
  // "tier" is "frontend" -> "tier=frontend"
  // "status" isNot "release" -> "status!=release"
  // "env" isIn List("staging", "production") -> "env in (staging,release)"
  // "env" isNotIn List("local", "dev") -> "env notin (local,dev)"
  final class StringExistsExpressionOps(key: String) extends ExistsExpression(key) {
    def doesNotExist: NotExistsExpression = NotExistsExpression(key)
    def is(value: String): IsEqualExpression = IsEqualExpression(key, value)
    def isNot(value: String): IsNotEqualExpression = IsNotEqualExpression(key, value)
    def in(values: String*): InExpression = InExpression(key, values)
    def isNotIn(values: List[String]): NotInExpression = NotInExpression(key, values)
  }

  implicit def stringToExpression(key: String): StringExistsExpressionOps = new StringExistsExpressionOps(key)
}
