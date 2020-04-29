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
  def egressOnly: ConnectionBuilder
  def ingressOnly: ConnectionBuilder
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

    override def egressOnly: ConnectionBuilder = ConnectionBuilder(
      resourceSelector = resourceSelector,
      ingress = NoSelector,
      egress = egress
    )
    override def ingressOnly: ConnectionBuilder = ConnectionBuilder(
      resourceSelector = resourceSelector,
      ingress = ingress,
      egress = NoSelector
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
    import Label.ops._

    def kubernetesDns: NamespaceSelector =
      SelectedNamespaces(
        expressions = Expressions((Name("kube-system") :: Nil).asExpressions: _*),
        protocols = Protocols(
          Protocol.Layers(l4 = UDP(53)),
          Protocol.Layers(l4 = TCP(53))
        )
      )

    def kubernetesApi: NamespaceSelector =
      SelectedNamespaces(
        expressions = Expressions((Name("default") :: Nil).asExpressions: _*),
        protocols = Protocols(Protocol.Layers(l4 = TCP(443)))
      )

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
            Expressions(other.labels.asExpressions: _*),
            Protocols.port(other.containers.flatMap(_.ports).map(TCP(_)): _*)
          )
        )
      }

      def communicatesWith(other: Namespace): ConnectionBuilder = {
        communicatesWith(
          SelectedNamespaces(
            Expressions(other.labels.asExpressions: _*),
            Protocols.Any
          )
        )
      }

      def communicatesWith(other: Selector): ConnectionBuilder = {
        // FIXME: un-HACK-me, hardcoded TCP
        val appPorts: Seq[Protocol.HasPort] = app.containers.flatMap(_.ports).map(TCP(_))
        val appProtocols = Protocols.port(appPorts: _*)
        ConnectionBuilder(
          resourceSelector = SelectedApplications(
            Expressions(app.labels.asExpressions: _*),
            appProtocols
          ),
          ingress = other,
          egress = other
        )
      }
    }
  }
}
