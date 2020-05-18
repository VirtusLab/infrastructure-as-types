package com.virtuslab.iat.kubernetes.dsl

import com.virtuslab.iat.dsl.IP.CIDR
import com.virtuslab.iat.dsl.Label.Name
import com.virtuslab.iat.dsl._
import com.virtuslab.iat.kubernetes.dsl.Application.IsApplication
import com.virtuslab.iat.kubernetes.dsl.Namespace.IsNamespace
import com.virtuslab.iat.kubernetes.dsl.NetworkPolicy.{ RuleEgress, RuleIngress }
import com.virtuslab.iat.kubernetes.dsl.Select.Selected

case class NetworkPolicy(
    labels: List[Label],
    podSelector: Expressions,
    ingress: List[RuleIngress],
    egress: List[RuleEgress])
  extends Labeled
  with Named
  with Patchable[NetworkPolicy]
  with Transformable[NetworkPolicy]
  with Interpretable[NetworkPolicy]

object NetworkPolicy {
  sealed trait RuleIngress
  final object DenyIngressRule extends RuleIngress
  final object AllowIngressRule extends RuleIngress
  final case class IngressRule(from: List[RulePeer], protocols: Protocols) extends RuleIngress
  object IngressRule {
    def apply[A, B](
        peer: Selection[A],
        other: Selection[B],
        otherRules: List[RulePeer]
      ): List[IngressRule] = IngressRule(otherRules, peer.protocols) :: Nil
  }

  sealed trait RuleEgress
  final object DenyEgressRule extends RuleEgress
  final object AllowEgressRule extends RuleEgress
  final case class EgressRule(to: List[RulePeer], protocols: Protocols) extends RuleEgress
  object EgressRule {
    def apply[A, B](
        peer: Selection[A],
        other: Selection[B],
        peerRules: List[RulePeer]
      ): List[EgressRule] = EgressRule(peerRules, other.protocols) :: Nil
  }

  sealed trait RulePeer
  final case class PodSelector(podSelector: Expressions) extends RulePeer
  final case class NamespaceSelector(namespaceSelector: Expressions) extends RulePeer
  final case class IPBlock(cidr: CIDR) extends RulePeer

  case class Builder(
      podSelector: Expressions,
      ingress: List[RuleIngress],
      egress: List[RuleEgress]) {
    def named(name: String): NetworkPolicy = labeled(Name(name) :: Nil)
    def labeled(labels: List[Label]): NetworkPolicy =
      NetworkPolicy(labels, podSelector, ingress, egress)
    def ingressOnly: Builder = Builder(podSelector, ingress, Nil)
    def egressOnly: Builder = Builder(podSelector, Nil, egress)
  }

  def apply[A](
      podSelector: Expressions,
      ingress: List[RuleIngress],
      egress: List[RuleEgress]
    ): Builder = Builder(podSelector, ingress, egress)
  def ingressOnly(podSelector: Expressions, ingress: List[RuleIngress]): Builder =
    Builder(podSelector, ingress, Nil)
  def egressOnly(podSelector: Expressions, egress: List[RuleEgress]): Builder =
    Builder(podSelector, Nil, egress)

  object ops {

    trait SelectionOpsIP[A] {
      def selection: Selection[A]
      protected def ipRules: List[IPBlock] = selection.protocols.cidrs.map(_.cidr).map(IPBlock).toList
    }

    implicit class SelectionOpsApp[A](val selection: Selection[A])(implicit ev: A =:= Application) extends SelectionOpsIP[A] {
      private def applicationRules: List[RulePeer] = PodSelector(selection.expressions) :: Nil
      private[NetworkPolicy] def rules: List[RulePeer] = applicationRules ++ ipRules
    }
    implicit class SelectionOpsNs[A](val selection: Selection[A])(implicit ev: A =:= Namespace) extends SelectionOpsIP[A] {
      private def namespaceRules: List[RulePeer] = NamespaceSelector(selection.expressions) :: Nil
      private[NetworkPolicy] def rules: List[RulePeer] = namespaceRules ++ ipRules
    }
    implicit class SelectionOpsAny[A](val selection: Selected.Any) extends SelectionOpsIP[scala.Any] {
      private[NetworkPolicy] def rules: List[RulePeer] = ipRules
    }
    implicit class SelectionOpsNone(val selection: Selected.None) extends SelectionOpsIP[scala.Nothing] {
      private[NetworkPolicy] def rules: List[RulePeer] = ipRules
    }

    implicit class NetworkPolicyOpsApp[A](peer: Selection[A])(implicit ev: A =:= Application) {
      def communicatesWith(other: IsApplication): Builder = Builder(
        peer.expressions,
        IngressRule(peer, other.selection, other.selection.rules),
        EgressRule(peer, other.selection, peer.rules)
      )
      def communicatesWith(other: IsNamespace): Builder = Builder(
        peer.expressions,
        IngressRule(peer, other.selection, other.selection.rules),
        EgressRule(peer, other.selection, peer.rules)
      )
      def communicatesWith(other: Selected.Any): Builder = Builder(
        peer.expressions,
        IngressRule(peer, other, other.rules),
        EgressRule(peer, other, peer.rules)
      )
      def communicatesWith(other: Selected.None): Builder = Builder(
        peer.expressions,
        IngressRule(peer, other, other.rules),
        EgressRule(peer, other, peer.rules)
      )
    }

    implicit class NetworkPolicyOpsAny(peer: Selected.Any) {
      def communicatesWith(other: IsApplication): Builder = Builder(
        peer.expressions,
        IngressRule(peer, other.selection, other.selection.rules),
        EgressRule(peer, other.selection, peer.rules)
      )
      def communicatesWith(other: IsNamespace): Builder = Builder(
        peer.expressions,
        IngressRule(peer, other.selection, other.selection.rules),
        EgressRule(peer, other.selection, peer.rules)
      )
      def communicatesWith(other: Selected.Any): Builder = Builder(
        peer.expressions,
        IngressRule(peer, other, other.rules),
        EgressRule(peer, other, peer.rules)
      )
      def communicatesWith(other: Selected.None): Builder = Builder(
        peer.expressions,
        IngressRule(peer, other, other.rules),
        EgressRule(peer, other, peer.rules)
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
          AllowIngressRule :: Nil
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
          AllowEgressRule :: Nil
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

    val denyExternalEgress = Select.any
      .communicatesWith(
        Select.any.withIPs(
          IP.Range("10.0.0.0/8"),
          IP.Range("172.16.0.0/12"),
          IP.Range("192.168.0.0/16")
        )
      )
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
      Select.any
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
