package com.virtuslab.dsl

import com.virtuslab.dsl.Expressions.Expression

trait Reference extends Labeled

abstract class Selectable {
  def expressions: Set[_ <: Expression]
  def asShortString: String
}

sealed trait Unselected extends Selectable
case object Unselected extends Unselected {
  override def expressions: Set[_ <: Expression] = Set()
  override def asShortString: String = "unselected"
}

trait Labeled extends Selectable {
  def name: String = labels.name.value
  def labels: Labels

  override def expressions: Set[_ <: Expression] = labels.values
  override def asShortString: String = labels.asShortString

  override def hashCode(): Int = labels.hashCode()
  override def equals(obj: Any): Boolean = obj match {
    case other: Labeled => labels.equals(other.labels)
    case _              => false
  }
}

trait Expressions extends Selectable {
  override def expressions: Set[_ <: Expression]
}

object Expressions {
  def apply(expressions: Expression*): Expressions = ExpressionsDefinition(expressions.toSet)

  final case class ExpressionsDefinition(expressions: Set[Expression]) extends Expressions {
    override def toString: String = expressions.mkString(",")
    override def asShortString: String = toString.take(20)
  }

  sealed trait Expression {
    val key: String
  }

  sealed trait ExistenceExpression extends Expression

  sealed trait EqualityExpression extends Expression {
    val value: String
  }

  sealed trait SetExpression extends Expression {
    val values: List[String]
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

  import scala.language.implicitConversions

  // this DSL enables equality and set based selector expressions analogous to the Kubernetes API
  // The following illustrates mappings from this DSL to k8s selector expressions syntax:
  // "production" -> "production"
  // '"production" doesNotExist ->  "!production"
  // "tier" is "frontend" -> "tier=frontend"
  // "status" isNot "release" -> "status!=release"
  // "env" isIn List("staging", "production") -> "env in (staging,release)"
  // "env" isNotIn List("local", "dev") -> "env notin (local,dev)"
  implicit def stringToExpression(key: String): ExistsExpression = new ExistsExpression(key) {
    def doesNotExist: NotExistsExpression = NotExistsExpression(key)
    def is(value: String): IsEqualExpression = IsEqualExpression(key, value)
    def isNot(value: String): IsNotEqualExpression = IsNotEqualExpression(key, value)
    def isIn(values: List[String]): InExpression = InExpression(key, values)
    def isNotIn(values: List[String]): NotInExpression = NotInExpression(key, values)
  }
  implicit def expressionToExpressions(expr: Expression*): Expressions = Expressions(expr: _*)
}

trait Label extends Expressions.EqualityExpression {
  override def toString: String = key + "=" + value
}

final case class Name(value: String) extends Label {
  val key: String = "name"
}

case class Labels(name: Name, values: Set[Label]) extends Expressions {
  import scala.collection.generic.CanBuildFrom

  def all: Set[Label] = Set(name) ++ values
  def toMap: Map[String, String] = all.map(l => l.key -> l.value).toMap
  def map[B, That](f: Label => B)(implicit bf: CanBuildFrom[Set[Label], B, That]): That = all.map(f)(bf)

  override def expressions: Set[_ <: Expression] = all.map(l => l)
  override def asShortString: String = name.value.take(20)
}

object Labels {
  def apply(name: Name, values: Label*): Labels = Labels(name, values.toSet)

  def applicationLabeled(expressions: Expression*): ApplicationSelector =
    ApplicationSelector(Expressions(expressions: _*))
  def namespaceLabeled(expressions: Expression*): NamespaceSelector =
    NamespaceSelector(Expressions(expressions: _*))
}

abstract class Selector {
  def selectable: Selectable
}

case class NamespaceSelector(selectable: Selectable) extends Selector
case class ApplicationSelector(selectable: Selectable) extends Selector

sealed trait EmptySelector extends Selector
case object EmptySelector extends EmptySelector {
  override def selectable: Unselected = Unselected
}

object Validation {
  // From Kubernetes:
  // https://github.com/kubernetes/kubernetes/blob/v1.18.0-rc.1/staging/src/k8s.io/apimachinery/pkg/util/validation/validation.go
  import scala.util.matching.Regex

  val qualifiedNameCharFmt: String = "[A-Za-z0-9]"
  val qualifiedNameExtCharFmt: String = "[-A-Za-z0-9_.]"
  val qualifiedNameFmt: String = "(" + qualifiedNameCharFmt + qualifiedNameExtCharFmt + "*)?" + qualifiedNameCharFmt
  val qualifiedNameMaxLength: Int = 63

  val qualifiedNameRegexp: Regex = ("^" + qualifiedNameFmt + "$").r

  def IsQualifiedName(value: String): (Boolean, String) = {
    if (value.length == 0) {
      return (false, "value is empty")
    }
    if (value.length > qualifiedNameMaxLength) {
      return (false, "value length exceeds " + qualifiedNameMaxLength + " characters")
    }
    if (!qualifiedNameRegexp.pattern.matcher(value).matches) {
      return (false, s"value doesn not match pattern '$qualifiedNameRegexp'")
    }
    (true, "")
  }
}
