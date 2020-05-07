package com.virtuslab.iat.dsl

import com.virtuslab.iat.dsl.Expressions.Expression

trait Expressions {
  def expressions: Set[Expression]
}

object Expressions {
  def apply(expressions: Expression*): Expressions = Some(expressions.toSet)
  def apply(expressions: Set[Expression]): Expressions = Some(expressions)

  sealed trait Any extends Expressions
  case object Any extends Any {
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
}
