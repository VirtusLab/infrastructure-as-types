package com.virtuslab.iat.kubernetes.dsl

import com.virtuslab.iat.dsl.Expressions.Expression
import com.virtuslab.iat.dsl.Label.Name
import com.virtuslab.iat.dsl.Peer.Selected
import com.virtuslab.iat.dsl.Traffic.{ Egress, Ingress }
import com.virtuslab.iat.dsl.{ Peer, _ }

case class NetworkPolicy[A <: Peer[A], B <: Peer[B]](
    labels: List[Label],
    peer: Peer[A],
    other: Peer[B],
    ingress: Option[Ingress[B, A]],
    egress: Option[Egress[A, B]])
  extends Labeled
  with Named
  with Patchable[NetworkPolicy[A, B]]
  with Transformable[NetworkPolicy[A, B]]
  with Interpretable[NetworkPolicy[A, B]]

object NetworkPolicy {
  case class Builder[A <: Peer[A], B <: Peer[B]](
      peer: Peer[A],
      other: Peer[B],
      ingress: Option[Ingress[B, A]],
      egress: Option[Egress[A, B]]) {
    def named(name: String): NetworkPolicy[A, B] = labeled(Name(name) :: Nil)
    def labeled(labels: List[Label]): NetworkPolicy[A, B] =
      NetworkPolicy(labels, peer, other, ingress, egress)
    def ingressOnly: Builder[A, B] = Builder(peer, other, ingress, None)
    def egressOnly: Builder[A, B] = Builder(peer, other, None, egress)
  }

  def apply[A <: Peer[A], B <: Peer[B]](peer: Peer[A], other: Peer[B]): NetworkPolicy.Builder[A, B] =
    NetworkPolicy.Builder[A, B](peer, other, Some(Ingress(other, peer)), Some(Egress(peer, other)))

  implicit class NetworkPolicyOps[A <: Peer[A]](peer: Peer[A]) {
    def communicatesWith[B <: Peer[B]](other: Peer[B]): NetworkPolicy.Builder[A, B] =
      NetworkPolicy(peer, other)
  }

  //noinspection TypeAnnotation
  object default {
    val allowAllIngress =
      NetworkPolicy(
        Peer.Any,
        Peer.Any
      ).ingressOnly.named("default-allow-all-ingress")

    val denyAllIngress =
      NetworkPolicy(
        Peer.Any,
        Peer.None
      ).ingressOnly.named("default-deny-all-ingress")

    val allowAllEgress =
      NetworkPolicy(
        Peer.Any,
        Peer.Any
      ).egressOnly.named("default-allow-all-egress")

    val denyAllEgress =
      NetworkPolicy(
        Peer.Any,
        Peer.None
      ).egressOnly.named("default-deny-all-egress")

    val denyAll =
      NetworkPolicy(
        Peer.None,
        Peer.None
      ).named("default-deny-all")

    val denyExternalEgress =
      NetworkPolicy(
        Peer.Any,
        SelectedIPs(
          IP.Range("10.0.0.0/8"),
          IP.Range("172.16.0.0/12"),
          IP.Range("192.168.0.0/16")
        )
      ).egressOnly.named("default-deny-external-egress")
  }

  import Label.ops._

  val kubernetesDns: Selected[Namespace] =
    Selected[Namespace](
      expressions = Expressions((Name("kube-system") :: Nil).asExpressions: _*),
      protocols = Protocols(
        Protocol.Layers(l4 = UDP(53)),
        Protocol.Layers(l4 = TCP(53))
      ),
      identities = Identities(ClusterDNS("kube-dns", "kube-system"))
    )

  val kubernetesApi: Selected[Namespace] =
    Selected[Namespace](
      expressions = Expressions((Name("default") :: Nil).asExpressions: _*),
      protocols = Protocols(
        Protocol.Layers(l4 = TCP(443))
      ),
      identities = Identities(ClusterDNS("kubernetes", "default"))
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
    ).withPorts(TCP(443))

  def applicationLabeled(expressions: Expressions): Selected[Application] =
    applicationLabeled(expressions.expressions.toList: _*)

  def applicationLabeled(expressions: Expression*): Selected[Application] =
    Selected[Application](
      expressions = Expressions(expressions: _*),
      protocols = Protocols.Any,
      identities = Identities.Any
    )

  def namespaceLabeled(expressions: Expressions): Selected[Namespace] =
    namespaceLabeled(expressions.expressions.toList: _*)

  def namespaceLabeled(expressions: Expression*): Selected[Namespace] =
    Selected[Namespace](
      expressions = Expressions(expressions: _*),
      protocols = Protocols.Any,
      identities = Identities.Any
    )
}
