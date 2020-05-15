package com.virtuslab.iat.skuber

import _root_.skuber.apps.v1.Deployment
import _root_.skuber.ext.{ Ingress => SIngress }
import _root_.skuber.networking.{ NetworkPolicy => SNetworkPolicy }
import _root_.skuber.{ ConfigMap, LabelSelector, ObjectMeta, Service, Namespace => SNamespace, Secret => SSecret }
import com.virtuslab.iat
import com.virtuslab.iat.dsl.Peer.Selected

trait DefaultInterpreters {
  import iat.dsl.Label.ops._
  import iat.dsl._
  import iat.kubernetes.dsl._
  import iat.scala.ops._

  implicit val namespaceInterpreter: Namespace => SNamespace =
    (ns: Namespace) =>
      SNamespace.from(
        ObjectMeta(
          name = ns.name,
          labels = ns.labels.asMap
        )
      )

  implicit val applicationInterpreter: (Application, Namespace) => (Service, Deployment) =
    (subinterpreter.serviceInterpreter _)
      .merge(subinterpreter.deploymentInterpreter)

  implicit val configurationInterpreter: (Configuration, Namespace) => ConfigMap =
    (obj: Configuration, ns: Namespace) => {
      ConfigMap(
        metadata = subinterpreter.objectMetaInterpreter(obj, ns),
        data = obj.data
      )
    }

  implicit val secretInterpreter: (Secret, Namespace) => SSecret =
    (obj: Secret, ns: Namespace) => {
      SSecret(
        metadata = subinterpreter.objectMetaInterpreter(obj, ns),
        data = obj.data.view.mapValues(_.getBytes).toMap
      )
    }

  implicit val gatewayInterpreter: (Gateway, Namespace) => SIngress = (obj: Gateway, ns: Namespace) => {
    SIngress(
      apiVersion = "networking.k8s.io/v1beta1", // Skuber uses wrong api version
      metadata = subinterpreter.objectMetaInterpreter(obj, ns),
      spec = obj.protocols match {
        case Protocols.Any => None
        case Protocols.Selected(layers) =>
          Some(
            SIngress.Spec(
              rules = layers.map {
                case Protocol.AnyLayers => SIngress.Rule(host = None, http = SIngress.HttpRule())
                case Protocol.SomeLayers(http: HTTP, tcp: TCP, _) =>
                  SIngress.Rule(
                    host = http.host.get,
                    http = SIngress.HttpRule(
                      paths = List(
                        SIngress.Path(
                          http.path.get.getOrElse("/"),
                          SIngress.Backend(
                            serviceName = "???", // FIXME get from identity
                            servicePort = tcp.port.get.get._1 // FIXME
                          )
                        )
                      )
                    )
                  )
              }.toList
              // TODO TLS
            )
          )
      }
    )
  }

  implicit def networkPolicyInterpreter[A <: Peer[A], B <: Peer[B]]: (NetworkPolicy[A, B], Namespace) => SNetworkPolicy =
    (obj: NetworkPolicy[A, B], ns: Namespace) =>
      SNetworkPolicy(
        metadata = subinterpreter.objectMetaInterpreter(obj, ns),
        spec = Some(
          SNetworkPolicy.Spec(
            podSelector = LabelSelector(subinterpreter.expressions(obj.peer.expressions): _*),
            ingress = obj.ingress match {
              case Some(ingress) => ingressRules(ingress.from, ingress.to)
              case None          => List.empty // NoSelector
            },
            egress = obj.egress match {
              case Some(egress) => egressRules(egress.from, egress.to)
              case None         => List.empty // NoSelector
            },
            policyTypes = (obj.ingress, obj.egress) match {
              case (Some(_), None)    => List("Ingress")
              case (None, Some(_))    => List("Egress")
              case (Some(_), Some(_)) => List("Ingress", "Egress")
              case (None, None)       => List()
            }
          )
        )
      )

  protected def ingressRules[A <: Peer[A], B <: Peer[B]](from: Peer[A], to: Peer[B]): List[SNetworkPolicy.IngressRule] = {
    (from.reference, to.reference) match {
      case (Peer.None, _) => List.empty // DenySelector, the difference is in 'policyTypes'
      case (Peer.Any, _)  => List(SNetworkPolicy.IngressRule()) // AllowSelector
      case (p: Peer[_], _) =>
        p.reference match {
          case _: Application                                     => ingressPodRules(from, to)
          case _: Namespace                                       => ingressNamespaceRules(from, to)
          case s: Selected[_] if s.hasType[Selected[Application]] => ingressPodRules(from, to)
          case s: Selected[_] if s.hasType[Selected[Namespace]]   => ingressNamespaceRules(from, to)
          case s: SelectedIPs                                     => ingressIPBocksRules(s)
          case s: SelectedIPsAndPorts                             => ingressIPBocksRules(s)
        }
    }
  }

  protected def egressRules[A <: Peer[A], B <: Peer[B]](from: Peer[A], to: Peer[B]): List[SNetworkPolicy.EgressRule] = {
    (from.reference, to.reference) match {
      case (_, Peer.None)       => List.empty // DenySelector, the difference is in 'policyTypes'
      case (Peer.Any, Peer.Any) => List(SNetworkPolicy.EgressRule()) // AllowSelector
      case (_, p: Peer[_]) =>
        p.reference match {
          case _: Application                                     => egressPodRules(from, to)
          case _: Namespace                                       => egressNamespaceRules(from, to)
          case s: Selected[_] if s.hasType[Selected[Application]] => egressPodRules(from, to)
          case s: Selected[_] if s.hasType[Selected[Namespace]]   => egressNamespaceRules(from, to)
          case s: SelectedIPs                                     => egressIPBlocksRules(s)
          case s: SelectedIPsAndPorts                             => egressIPBlocksRules(s)
        }
    }
  }

  protected def ingressPodRules[A <: Peer[A], B <: Peer[B]](from: Peer[A], to: Peer[B]): List[SNetworkPolicy.IngressRule] = List(
    SNetworkPolicy.IngressRule(
      from = List(
        SNetworkPolicy.Peer(
          podSelector = Some(
            LabelSelector(
              subinterpreter.expressions(from.expressions): _* // ingress from a labeled source
            )
          ),
          namespaceSelector = None,
          ipBlock = None
        )
        // TODO multiple "Peers", combined with logical OR
      ),
      ports = subinterpreter.ports(to.protocols) // to the ports of our selected pod/namespace
    )
    // TODO multiple "Rules", combined with logical OR
  )

  protected def ingressNamespaceRules[A <: Peer[A], B <: Peer[B]](from: Peer[A], to: Peer[B]): List[SNetworkPolicy.IngressRule] =
    List(
      SNetworkPolicy.IngressRule(
        from = List(
          SNetworkPolicy.Peer(
            podSelector = None,
            namespaceSelector = Some(
              LabelSelector(
                subinterpreter.expressions(from.expressions): _* // ingress from a labeled source
              )
            ),
            ipBlock = None
          )
          // TODO multiple "Peers", combined with logical OR
        ),
        ports = subinterpreter.ports(to.protocols) // to the ports of our selected pod/namespace
      )
      // TODO multiple "Rules", combined with logical OR
    )

  protected def ingressIPBocksRules[A <: Peer[A]](s: Peer[A] with CIDRs): List[SNetworkPolicy.IngressRule] = List(
    SNetworkPolicy.IngressRule(
      from = subinterpreter.ipBlocks(s),
      ports = subinterpreter.ports(s.protocols)
    )
    // TODO multiple "Rules", combined with logical OR
  )

  protected def egressPodRules[A <: Peer[A], B <: Peer[B]](from: Peer[A], to: Peer[B]): List[SNetworkPolicy.EgressRule] =
    List(
      SNetworkPolicy.EgressRule(
        to = List(
          SNetworkPolicy.Peer(
            podSelector = Some(
              LabelSelector(
                subinterpreter.expressions(to.expressions): _* // egress to a labeled target
              )
            ),
            namespaceSelector = None,
            ipBlock = None
          )
          // TODO multiple "Peers", combined with logical OR
        ),
        ports = subinterpreter.ports(to.protocols) // to the ports of the target
      )
      // TODO multiple "Rules", combined with logical OR
    )

  protected def egressNamespaceRules[A <: Peer[A], B <: Peer[B]](from: Peer[A], to: Peer[B]): List[SNetworkPolicy.EgressRule] =
    List(
      SNetworkPolicy.EgressRule(
        to = List(
          SNetworkPolicy.Peer(
            podSelector = None,
            namespaceSelector = Some(
              LabelSelector(
                subinterpreter.expressions(to.expressions): _* // egress to a labeled target
              )
            ),
            ipBlock = None
          )
          // TODO multiple "Peers", combined with logical OR
        ),
        ports = subinterpreter.ports(to.protocols) // to the ports of the target
      )
      // TODO multiple "Rules", combined with logical OR
    )

  protected def egressIPBlocksRules[A <: Peer[A]](s: Peer[A] with CIDRs): List[SNetworkPolicy.EgressRule] = List(
    SNetworkPolicy.EgressRule(
      to = subinterpreter.ipBlocks(s),
      ports = subinterpreter.ports(s.protocols)
    )
    // TODO multiple "Rules", combined with logical OR
  )
}
