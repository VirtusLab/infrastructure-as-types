package com.virtuslab.dsl.interpreter

import com.virtuslab.dsl.Connection.ConnectionDefinition
import com.virtuslab.dsl._
import skuber.networking.NetworkPolicy
import skuber.networking.NetworkPolicy.{ EgressRule, IngressRule, Peer, Spec }
import skuber.{ LabelSelector, ObjectMeta }

class ConnectionInterpreter(expressions: LabelExpressionInterpreter) {

  def apply(connection: ConnectionDefinition): NetworkPolicy = {
    NetworkPolicy(
      metadata = ObjectMeta(
        name = connection.name,
        namespace = connection.namespace.name,
        labels = connection.labels.toMap
      ),
      spec = Some(
        Spec(
          podSelector = connection.resourceSelector match {
            case s: Selector =>
              LabelSelector(
                expressions(s.selectable.expressions): _*
              )
          },
          ingress = connection.ingress match {
            case _: EmptySelector => List()
            case s: ApplicationSelector =>
              List(
                IngressRule(
                  from = List(
                    Peer(
                      podSelector = Some(
                        LabelSelector(
                          expressions(s.selectable.expressions): _*
                        )
                      )
                    )
                  )
                )
              )
            case s: NamespaceSelector =>
              List(
                IngressRule(
                  from = List(
                    Peer(
                      namespaceSelector = Some(
                        LabelSelector(
                          expressions(s.selectable.expressions): _*
                        )
                      )
                    )
                  )
                )
              )
          },
          egress = connection.ingress match {
            case _: EmptySelector => List()
            case s: ApplicationSelector =>
              List(
                EgressRule(
                  to = List(
                    Peer(
                      podSelector = Some(
                        LabelSelector(
                          expressions(s.selectable.expressions): _*
                        )
                      )
                    )
                  )
                )
              )
            case s: NamespaceSelector =>
              List(
                EgressRule(
                  to = List(
                    Peer(
                      namespaceSelector = Some(
                        LabelSelector(
                          expressions(s.selectable.expressions): _*
                        )
                      )
                    )
                  )
                )
              )
          },
          policyTypes = List("Ingress", "Egress")
        )
      )
    )
  }
}

class LabelExpressionInterpreter {
  import com.virtuslab.dsl.Expressions._

  def apply(es: Expressions): Seq[LabelSelector.Requirement] =
    apply(es.expressions)

  def apply(es: Set[Expression]): Seq[LabelSelector.Requirement] = {
    es.map {
      case l: Label                => LabelSelector.IsEqualRequirement(l.key, l.value)
      case e: ExistsExpression     => LabelSelector.ExistsRequirement(e.key)
      case e: NotExistsExpression  => LabelSelector.NotExistsRequirement(e.key)
      case e: IsEqualExpression    => LabelSelector.IsEqualRequirement(e.key, e.value)
      case e: IsNotEqualExpression => LabelSelector.IsNotEqualRequirement(e.key, e.value)
      case e: InExpression         => LabelSelector.InRequirement(e.key, e.values.toList)
      case e: NotInExpression      => LabelSelector.NotInRequirement(e.key, e.values.toList)
    }.toSeq
  }
}
