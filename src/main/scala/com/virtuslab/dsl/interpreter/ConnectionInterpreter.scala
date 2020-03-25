package com.virtuslab.dsl.interpreter

import com.virtuslab.dsl.interpreter.LabelExpressionInterpreter.expressionsToRequirements
import com.virtuslab.dsl.{ ApplicationSelector, Connection, DistributedSystem, EmptySelector, Expressions, NamespaceSelector, Selectable, Selector }
import skuber.{ LabelSelector, ObjectMeta }
import skuber.networking.NetworkPolicy
import skuber.networking.NetworkPolicy.{ EgressRule, IngressRule, Peer, Spec }

class ConnectionInterpreter(system: DistributedSystem) {

  def apply(connection: Connection): NetworkPolicy = {
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
                expressionsToRequirements(s.selectable.expressions): _*
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
                          expressionsToRequirements(s.selectable.expressions): _*
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
                          expressionsToRequirements(s.selectable.expressions): _*
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
                          expressionsToRequirements(s.selectable.expressions): _*
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
                          expressionsToRequirements(s.selectable.expressions): _*
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

object LabelExpressionInterpreter {
  import com.virtuslab.dsl.Expressions.Expression

  def expressionsToRequirements(es: Set[_ <: Expression]): Seq[LabelSelector.Requirement] = {
    es.map {
      case e: Expressions.EqualityExpression => LabelSelector.IsEqualRequirement(e.key, e.value)
    }.toSeq
  }
}
