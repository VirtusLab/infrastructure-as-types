package com.virtuslab.interpreter

import com.virtuslab.dsl.{ AllowSelector, ApplicationSelector, CIDRs, Configuration, Connection, Definition, DenySelector, Expressions, IP, Label, NamespaceSelector, NoSelector, Protocol, Protocols, SelectedIPs, SelectedIPsAndPorts, Selector, TCP, UDP }
import com.virtuslab.exporter.skuber.Resource
import skuber.json.format._
import skuber.ConfigMap
import skuber.networking.NetworkPolicy
import skuber.networking.NetworkPolicy.{ EgressRule, IPBlock, IngressRule, Peer, Port, Spec }
import skuber.{ LabelSelector, ObjectMeta }

object Skuber {

  class SkuberContext extends Context {
    override type Ret[A] = Seq[Resource[A]]
  }

  implicit val context: SkuberContext = new SkuberContext

  implicit val configurationInterpreter: Interpreter[SkuberContext, Configuration] =
    (cfg: Definition[SkuberContext, Configuration]) => {
      Seq(
        Resource(
          ConfigMap(
            metadata = ObjectMeta(
              name = cfg.obj.name,
              namespace = cfg.namespace.name,
              labels = cfg.obj.labels.toMap
            ),
            data = cfg.obj.data
          )
        )
      )
    }

  implicit val connectionInterpreter: Interpreter[SkuberContext, Connection] =
    (connection: Definition[SkuberContext, Connection]) => {
      Seq(
        Resource(
          NetworkPolicy(
            metadata = ObjectMeta(
              name = connection.obj.name,
              namespace = connection.namespace.name,
              labels = connection.obj.labels.toMap
            ),
            spec = Some(
              Spec(
                podSelector = connection.obj.resourceSelector match {
                  // NoSelector and AllowSelector are interchangeable here
                  case s: Selector =>
                    LabelSelector(
                      expressions(s.expressions): _*
                    )
                },
                ingress = connection.obj.ingress match {
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
                            ),
                            namespaceSelector = None,
                            ipBlock = None
                          )
                          // TODO multiple "Peers", combined with logical OR
                        ),
                        ports = ports(s.protocols)
                      )
                      // TODO multiple "Rules", combined with logical OR
                    )
                  case s: NamespaceSelector =>
                    List(
                      IngressRule(
                        from = List(
                          Peer(
                            podSelector = None,
                            namespaceSelector = Some(
                              LabelSelector(
                                expressions(s.expressions): _*
                              )
                            ),
                            ipBlock = None
                          )
                          // TODO multiple "Peers", combined with logical OR
                        ),
                        ports = ports(s.protocols)
                      )
                      // TODO multiple "Rules", combined with logical OR
                    )
                  case s: SelectedIPs =>
                    List(
                      IngressRule(
                        from = ipBlocks(s),
                        ports = ports(s.protocols)
                      )
                    )
                  case s: SelectedIPsAndPorts =>
                    List(
                      IngressRule(
                        from = ipBlocks(s),
                        ports = ports(s.protocols)
                      )
                      // TODO multiple "Rules", combined with logical OR
                    )
                },
                egress = connection.obj.egress match {
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
                            namespaceSelector = None,
                            ipBlock = None
                          )
                          // TODO multiple "Peers", combined with logical OR
                        ),
                        ports = ports(s.protocols)
                      )
                      // TODO multiple "Rules", combined with logical OR
                    )
                  case s: NamespaceSelector =>
                    List(
                      EgressRule(
                        to = List(
                          Peer(
                            podSelector = None,
                            namespaceSelector = Some(
                              LabelSelector(
                                expressions(s.expressions): _*
                              )
                            ),
                            ipBlock = None
                          )
                          // TODO multiple "Peers", combined with logical OR
                        ),
                        ports = ports(s.protocols)
                      )
                      // TODO multiple "Rules", combined with logical OR
                    )
                  case s: SelectedIPs =>
                    List(
                      EgressRule(
                        to = ipBlocks(s),
                        ports = ports(s.protocols)
                      )
                      // TODO multiple "Rules", combined with logical OR
                    )
                  case s: SelectedIPsAndPorts =>
                    List(
                      EgressRule(
                        to = ipBlocks(s),
                        ports = ports(s.protocols)
                      )
                      // TODO multiple "Rules", combined with logical OR
                    )
                },
                policyTypes = (connection.obj.ingress, connection.obj.egress) match {
                  case (NoSelector, NoSelector) => List()
                  case (_, NoSelector)          => List("Ingress")
                  case (NoSelector, _)          => List("Egress")
                  case (_, _)                   => List("Ingress", "Egress")
                }
              )
            )
          )
        )
      )
    }

  private def ipBlocks(s: CIDRs): List[Peer] = {
    s.ips.map {
      case IP.RangeWithExceptions(ip, mask, exceptions) =>
        Peer(
          podSelector = None,
          namespaceSelector = None,
          ipBlock = Some(IPBlock(s"$ip/$mask", exceptions.map(e => s"${e.ip}/${e.mask}").toList))
        )
      case cidr: IP.CIDR =>
        Peer(
          podSelector = None,
          namespaceSelector = None,
          ipBlock = Some(IPBlock(s"${cidr.ip}/${cidr.mask}"))
        )
    }.toList
  }

  object expressions {
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

  object ports {
    def apply(ps: Protocols): List[Port] = ps.protocols.flatMap(layer => apply(layer.l4)).toList

    def apply(p: Protocol.L4): Option[Port] = p match {
      case UDP(port) => Some(Port(port.numberOrName, skuber.Protocol.UDP))
      case TCP(port) => Some(Port(port.numberOrName, skuber.Protocol.TCP))
      case _         => None
    }
  }
}
