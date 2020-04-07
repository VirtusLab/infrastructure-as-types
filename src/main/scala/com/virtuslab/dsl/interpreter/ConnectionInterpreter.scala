package com.virtuslab.dsl.interpreter

import com.virtuslab.dsl.Connection.ConnectionDefinition
import com.virtuslab.dsl._
import skuber.networking.NetworkPolicy
import skuber.networking.NetworkPolicy.{ EgressRule, IPBlock, IngressRule, Peer, Port, Spec }
import skuber.{ LabelSelector, ObjectMeta }

class ConnectionInterpreter(expressions: LabelExpressionInterpreter, ports: NetworkPortsInterpreter) {

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
            // NoSelector and AllowSelector are interchangeable here
            case s: Selector =>
              LabelSelector(
                expressions(s.expressions.expressions): _*
              )
          },
          ingress = connection.ingress match {
            case NoSelector    => List()
            case DenySelector  => List() // the difference is in 'policyTypes'
            case AllowSelector => List(IngressRule())
            case s: ApplicationSelector =>
              List(
                IngressRule(
                  from = List(
                    Peer(
                      podSelector = Some(
                        LabelSelector(
                          expressions(s.expressions): _*
                        )
                      )
                    )
                  ),
                  ports = ports(s.protocols)
                )
              )
            case s: NamespaceSelector =>
              List(
                IngressRule(
                  from = List(
                    Peer(
                      namespaceSelector = Some(
                        LabelSelector(
                          expressions(s.expressions): _*
                        )
                      ),
                      ipBlock = None // TODO
                    )
                  ),
                  ports = ports(s.protocols)
                )
              )
            case s: SelectedIPs =>
              List(
                IngressRule(
                  from = ipBlocks(s),
                  ports = ports(s.protocols)
                )
              )
          },
          egress = connection.egress match {
            case NoSelector    => List()
            case DenySelector  => List() // the difference is in 'policyTypes'
            case AllowSelector => List(EgressRule())
            case s: ApplicationSelector =>
              List(
                EgressRule(
                  to = List(
                    Peer(
                      podSelector = Some(
                        LabelSelector(
                          expressions(s.expressions): _*
                        )
                      ),
                      ipBlock = None // TODO
                    )
                  ),
                  ports = ports(s.protocols)
                )
              )
            case s: NamespaceSelector =>
              List(
                EgressRule(
                  to = List(
                    Peer(
                      namespaceSelector = Some(
                        LabelSelector(
                          expressions(s.expressions): _*
                        )
                      ),
                      ipBlock = None // TODO
                    )
                  ),
                  ports = ports(s.protocols)
                )
              )
            case s: SelectedIPs =>
              List(
                EgressRule(
                  to = ipBlocks(s),
                  ports = ports(s.protocols)
                )
              )
          },
          policyTypes = (connection.ingress, connection.egress) match {
            case (NoSelector, NoSelector) => List()
            case (_, NoSelector)          => List("Ingress")
            case (NoSelector, _)          => List("Egress")
            case (_, _)                   => List("Ingress", "Egress")
          }
        )
      )
    )
  }

  private def ipBlocks(s: SelectedIPs) = {
    s.ips.map {
      case IP.RangeWithExceptions(ip, mask, exceptions) =>
        Peer(
          ipBlock = Some(IPBlock(s"$ip/$mask", exceptions.map(e => s"${e.ip}/${e.mask}").toList))
        )
      case cidr: IP.CIDR =>
        Peer(
          ipBlock = Some(IPBlock(s"${cidr.ip}/${cidr.mask}"))
        )
    }.toList
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

class NetworkPortsInterpreter {
  def apply(ps: Protocols): List[Port] = ps.protocols.flatMap(layer => apply(layer.l4)).toList

  def apply(p: Protocol.L4): Option[Port] = p match {
    case UDP(port, _) => Some(Port(port.numberOrName, skuber.Protocol.UDP))
    case TCP(port, _) => Some(Port(port.numberOrName, skuber.Protocol.TCP))
    case _            => None
  }
}
