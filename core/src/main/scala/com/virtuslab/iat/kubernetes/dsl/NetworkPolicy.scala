package com.virtuslab.iat.kubernetes.dsl

import com.virtuslab.iat.dsl.IP.CIDR
import com.virtuslab.iat.dsl.Label.Name
import com.virtuslab.iat.dsl.Peer.Selected
import com.virtuslab.iat.dsl._
import com.virtuslab.iat.kubernetes.dsl.Application.IsApplication
import com.virtuslab.iat.kubernetes.dsl.Namespace.IsNamespace
import com.virtuslab.iat.kubernetes.dsl.NetworkPolicy.{ RuleEgress, RuleIngress }

case class NetworkPolicy(
    labels: Seq[Label],
    podSelector: Expressions,
    ingress: Seq[RuleIngress],
    egress: Seq[RuleEgress])
  extends Labeled
  with Named
  with Patchable[NetworkPolicy]
  with Interpretable[NetworkPolicy]

object NetworkPolicy {
  sealed trait RuleIngress
  final object DenyIngressRule extends RuleIngress
  final case class AllowIngressRule(protocols: Protocols) extends RuleIngress
  final case class IngressRule(from: Seq[RulePeer], protocols: Protocols) extends RuleIngress
  object IngressRule {
    def apply[A, B](
        peer: Selection[A],
        other: Selection[B],
        otherRules: Seq[RulePeer]
      ): Seq[IngressRule] = IngressRule(otherRules, peer.protocols) :: Nil
  }

  sealed trait RuleEgress
  final object DenyEgressRule extends RuleEgress
  final case class AllowEgressRule(protocols: Protocols) extends RuleEgress
  final case class EgressRule(to: Seq[RulePeer], protocols: Protocols) extends RuleEgress
  object EgressRule {
    def apply[A, B](
        peer: Selection[A],
        other: Selection[B],
        peerRules: Seq[RulePeer]
      ): Seq[EgressRule] = EgressRule(peerRules, other.protocols) :: Nil
  }

  sealed trait RulePeer
  final case class PodSelector(podSelector: Expressions) extends RulePeer
  final case class NamespaceSelector(namespaceSelector: Expressions) extends RulePeer
  final case class IPBlock(cidr: CIDR) extends RulePeer

  case class Builder(
      podSelector: Expressions,
      ingress: Seq[RuleIngress],
      egress: Seq[RuleEgress]) {
    def named(name: String): NetworkPolicy = labeled(Name(name) :: Nil)
    def labeled(labels: Seq[Label]): NetworkPolicy =
      NetworkPolicy(labels, podSelector, ingress, egress)
    def ingressOnly: Builder = Builder(podSelector, ingress, Nil)
    def egressOnly: Builder = Builder(podSelector, Nil, egress)
  }

  def apply[A](
      podSelector: Expressions,
      ingress: Seq[RuleIngress],
      egress: Seq[RuleEgress]
    ): Builder = Builder(podSelector, ingress, egress)
  def ingressOnly(podSelector: Expressions, ingress: Seq[RuleIngress]): Builder =
    Builder(podSelector, ingress, Nil)
  def egressOnly(podSelector: Expressions, egress: Seq[RuleEgress]): Builder =
    Builder(podSelector, Nil, egress)

  object ops {

    trait SelectionOpsIP[A] {
      def selection: Selection[A]
      protected def ipRules: Seq[IPBlock] = selection.protocols.cidrs.map(_.cidr).map(IPBlock).toList
    }

    implicit class SelectionOpsApp[A](val selection: Selection[A])(implicit ev: A =:= Application) extends SelectionOpsIP[A] {
      private def applicationRules: Seq[RulePeer] = PodSelector(selection.expressions) :: Nil
      private[NetworkPolicy] def rules: Seq[RulePeer] = applicationRules ++ ipRules
    }
    implicit class SelectionOpsNs[A](val selection: Selection[A])(implicit ev: A =:= Namespace) extends SelectionOpsIP[A] {
      private def namespaceRules: Seq[RulePeer] = NamespaceSelector(selection.expressions) :: Nil
      private[NetworkPolicy] def rules: Seq[RulePeer] = namespaceRules ++ ipRules
    }
    implicit class SelectionOpsAny[A](val selection: Selected.Any) extends SelectionOpsIP[scala.Any] {
      private[NetworkPolicy] def rules: Seq[RulePeer] = ipRules
    }
    implicit class SelectionOpsNone(val selection: Selected.None) extends SelectionOpsIP[scala.Nothing] {
      private[NetworkPolicy] def rules: Seq[RulePeer] = ipRules
    }

    implicit class NetworkPolicyOpsApp[A](peer: Selection[A])(implicit ev: A =:= Application) {
      def communicatesWith(other: IsApplication): Builder = Builder(
        peer.expressions,
        IngressRule(peer, other.selection, other.selection.rules),
        EgressRule(peer, other.selection, other.selection.rules)
      )
      def communicatesWith(other: IsNamespace): Builder = Builder(
        peer.expressions,
        IngressRule(peer, other.selection, other.selection.rules),
        EgressRule(peer, other.selection, other.selection.rules)
      )
      def communicatesWith(other: Selected.Any): Builder = Builder(
        peer.expressions,
        IngressRule(peer, other, other.rules),
        EgressRule(peer, other, other.rules)
      )
      def communicatesWith(other: Selected.None): Builder = Builder(
        peer.expressions,
        IngressRule(peer, other, other.rules),
        EgressRule(peer, other, other.rules)
      )
    }

    implicit class NetworkPolicyOpsAny(peer: Selected.Any) {
      def communicatesWith(other: IsApplication): Builder = Builder(
        peer.expressions,
        IngressRule(peer, other.selection, other.selection.rules),
        EgressRule(peer, other.selection, other.selection.rules)
      )
      def communicatesWith(other: IsNamespace): Builder = Builder(
        peer.expressions,
        IngressRule(peer, other.selection, other.selection.rules),
        EgressRule(peer, other.selection, other.selection.rules)
      )
      def communicatesWith(other: Selected.Any): Builder = Builder(
        peer.expressions,
        IngressRule(peer, other, other.rules),
        EgressRule(peer, other, other.rules)
      )
      def communicatesWith(other: Selected.None): Builder = Builder(
        peer.expressions,
        IngressRule(peer, other, other.rules),
        EgressRule(peer, other, other.rules)
      )
    }
  }

  //noinspection TypeAnnotation
  object default {
    import NetworkPolicy.ops._

    val allowAllIngress =
      NetworkPolicy
        .ingressOnly(
          Expressions.Any,
          AllowIngressRule(Protocols.Any) :: Nil
        )
        .named("default-allow-all-ingress")

    val denyAllIngress =
      NetworkPolicy
        .ingressOnly(
          Expressions.Any,
          DenyIngressRule :: Nil
        )
        .named("default-deny-all-ingress")

    val allowAllEgress =
      NetworkPolicy
        .egressOnly(
          Expressions.Any,
          AllowEgressRule(Protocols.Any) :: Nil
        )
        .named("default-allow-all-egress")

    val denyAllEgress =
      NetworkPolicy
        .egressOnly(
          Expressions.Any,
          DenyEgressRule :: Nil
        )
        .named("default-deny-all-egress")

    val denyAll =
      NetworkPolicy(
        Expressions.Any,
        DenyIngressRule :: Nil,
        DenyEgressRule :: Nil
      ).named("default-deny-all")

    val denyExternalEgress = Peer.any
      .communicatesWith(
        Peer.any.withIPs(
          IP.Range("10.0.0.0/8"),
          IP.Range("172.16.0.0/12"),
          IP.Range("192.168.0.0/16")
        )
      )
      .egressOnly
      .named("default-deny-external-egress")
  }

  object peer {
    import Label.ops._

    val kubernetesDns: Selection[Namespace] =
      Selected[Namespace](
        expressions = Expressions((Name("kube-system") :: Nil).asExpressions: _*),
        protocols = Protocols.ports(UDP(53), TCP(53)),
        identities = Identities(ClusterDNS("kube-dns", "kube-system"))
      )

    val kubernetesApi: Selection[Namespace] =
      Selected[Namespace](
        expressions = Expressions((Name("default") :: Nil).asExpressions: _*),
        protocols = Protocols.ports(TCP(443)),
        identities = Identities(ClusterDNS("kubernetes", "default"))
      )

    val external443: Selection[Any] =
      Peer.any
        .withIPs(
          IP.Range("0.0.0.0/0")
            .except(
              IP.Range("0.0.0.0/8"),
              IP.Range("10.0.0.0/8"),
              IP.Range("172.16.0.0/12"),
              IP.Range("192.168.0.0/16")
            )
        )
        .withPorts(TCP(443))
  }
}
