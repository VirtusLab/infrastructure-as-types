package com.virtuslab.iat.kubernetes.dsl

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

  object default {
    val allowAllIngress: Connection =
      Connection(
        name = "default-allow-all-ingress",
        resourceSelector = NoSelector,
        ingress = AllowSelector,
        egress = NoSelector
      )

    val denyAllIngress: Connection =
      Connection(
        name = "default-deny-all-ingress",
        resourceSelector = NoSelector,
        ingress = DenySelector,
        egress = NoSelector
      )

    val allowAllEgress: Connection =
      Connection(
        name = "default-allow-all-egress",
        resourceSelector = NoSelector,
        ingress = NoSelector,
        egress = AllowSelector
      )

    val denyAllEgress: Connection =
      Connection(
        name = "default-deny-all-egress",
        resourceSelector = NoSelector,
        ingress = NoSelector,
        egress = DenySelector
      )

    val denyAll: Connection =
      Connection(
        name = "default-deny-all",
        resourceSelector = NoSelector,
        ingress = DenySelector,
        egress = DenySelector
      )

    val denyExternalEgress: Connection =
      Connection(
        name = "default-deny-external-egress",
        resourceSelector = NoSelector,
        ingress = NoSelector,
        egress = SelectedIPs(
          IP.Range("10.0.0.0/8"),
          IP.Range("172.16.0.0/12"),
          IP.Range("192.168.0.0/16")
        )
      )
  }

  import Label.ops._

  val kubernetesDns: NamespaceSelector =
    SelectedNamespaces(
      expressions = Expressions((Name("kube-system") :: Nil).asExpressions: _*),
      protocols = Protocols(
        Protocol.Layers(l4 = UDP(53)),
        Protocol.Layers(l4 = TCP(53))
      )
    )

  val kubernetesApi: NamespaceSelector =
    SelectedNamespaces(
      expressions = Expressions((Name("default") :: Nil).asExpressions: _*),
      protocols = Protocols(Protocol.Layers(l4 = TCP(443)))
    )

  val external443: SelectedIPsAndPorts =
    SelectedIPs(
      IP.Range("0.0.0.0/0")
        .except(
          IP.Range("0.0.0.0/8"),
          IP.Range("10.0.0.0/8"),
          IP.Range("172.16.0.0/12"),
          IP.Range("192.168.0.0/16")
        )
    ).ports(TCP(443))

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
