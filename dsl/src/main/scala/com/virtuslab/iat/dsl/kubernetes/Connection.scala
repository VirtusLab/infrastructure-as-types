package com.virtuslab.iat.dsl.kubernetes

import com.virtuslab.iat.dsl.Expressions.Expression
import com.virtuslab.iat.dsl.Label.Name
import com.virtuslab.iat.dsl._

case class Connection(
    labels: List[Label],
    resourceSelector: Selector,
    ingress: Selector,
    egress: Selector)
  extends Named
  with Labeled
  with Patchable[Connection]
  with Interpretable[Connection]

trait ConnectionBuilder {
  def named(name: String): Connection
  def labeled(labels: List[Label]): Connection
}
object ConnectionBuilder {
  def apply(
      resourceSelector: Selector,
      ingress: Selector,
      egress: Selector
    ): ConnectionBuilder = new ConnectionBuilder {
    override def named(name: String): Connection = labeled(Name(name) :: Nil)
    override def labeled(labels: List[Label]): Connection = Connection(
      labels = labels,
      resourceSelector = resourceSelector,
      ingress = ingress,
      egress = egress
    )
  }
}

object Connection {
  def apply(
      name: String,
      resourceSelector: Selector,
      ingress: Selector,
      egress: Selector
    ): Connection = Connection(
    labels = Name(name) :: Nil,
    resourceSelector,
    ingress,
    egress
  )

  object ops {
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

    implicit class ApplicationConnectionOps(app: Application) {
      def communicatesWith(other: Application): ConnectionBuilder = {
        communicatesWith(
          SelectedApplications(
            Expressions(other.labels.map(l => Expressions.IsEqualExpression(l.key, l.value)): _*),
            Protocols.Any
          )
        )
      }

      def communicatesWith(other: Namespace): ConnectionBuilder = {
        communicatesWith(
          SelectedNamespaces(
            Expressions(other.labels.map(l => Expressions.IsEqualExpression(l.key, l.value)): _*),
            Protocols.Any
          )
        )
      }

      def communicatesWith(other: Selector): ConnectionBuilder = {
        ConnectionBuilder(
          resourceSelector = SelectedApplications(
            Expressions(app.labels.map(l => Expressions.IsEqualExpression(l.key, l.value)): _*),
            Protocols.Any
          ),
          ingress = other,
          egress = SelectedApplications(
            Expressions(app.labels.map(l => Expressions.IsEqualExpression(l.key, l.value)): _*),
            Protocols.Any
          )
        )
      }
    }
  }
}
