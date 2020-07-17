package com.virtuslab.iat.skuber

import _root_.skuber.apps.v1.Deployment
import _root_.skuber.ext.{ Ingress => SIngress }
import _root_.skuber.networking.{ NetworkPolicy => SNetworkPolicy }
import _root_.skuber.{ ConfigMap, LabelSelector, ObjectMeta, Service, Namespace => SNamespace, Secret => SSecret }
import com.virtuslab.iat
import com.virtuslab.iat.kubernetes.dsl.NetworkPolicy._

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
        data = obj.data.mapValues(_.getBytes).toMap
      )
    }

  implicit val gatewayInterpreter: (Gateway, Namespace) => SIngress = (obj: Gateway, ns: Namespace) => {
    def interpretOutputs(inputHttp: HTTP, outputs: Protocols): List[SIngress.Rule] = outputs match {
      case Protocols.None => SIngress.Rule(host = None, http = SIngress.HttpRule()) :: Nil
      case Protocols.Selected(layers) =>
        layers.map {
          case Protocol.SomeLayers(http: HTTP, tcp: TCP, _) =>
            SIngress.Rule(
              host = inputHttp.host.get,
              http = SIngress.HttpRule(
                paths = List(
                  SIngress.Path(
                    http.path.get.getOrElse("/"),
                    SIngress.Backend(
                      serviceName = http.host.get.get, // FIXME
                      servicePort = tcp.port.get.get._1 // FIXME
                    )
                  )
                )
              )
            )
        }.toList
    }

    SIngress(
      apiVersion = "extensions/v1beta1", // Skuber uses wrong api version
      metadata = subinterpreter.objectMetaInterpreter(obj, ns),
      spec = obj.inputs match {
        case Protocols.Any => None
        case Protocols.Selected(inLayers) =>
          Some(
            SIngress.Spec(
              rules = inLayers.flatMap {
                case Protocol.SomeLayers(inputHttp: HTTP, _: TCP, _) => interpretOutputs(inputHttp, obj.outputs)
                // TODO TLS
              }.toList
            )
          )
      }
    )
  }

  implicit val networkPolicyInterpreter: (NetworkPolicy, Namespace) => SNetworkPolicy =
    (obj: NetworkPolicy, ns: Namespace) =>
      SNetworkPolicy(
        metadata = subinterpreter.objectMetaInterpreter(obj, ns),
        spec = Some(
          SNetworkPolicy.Spec(
            podSelector = LabelSelector(subinterpreter.expressions(obj.podSelector): _*),
            ingress = obj.ingress.flatMap {
              case DenyIngressRule => Nil
              case AllowIngressRule(protocols) =>
                SNetworkPolicy.IngressRule(ports = subinterpreter.ports(protocols)) :: Nil
              case IngressRule(from, protocols) =>
                SNetworkPolicy.IngressRule(
                  from = from.map {
                    case PodSelector(podSelector) =>
                      SNetworkPolicy.Peer(
                        podSelector = Some(
                          LabelSelector(subinterpreter.expressions(podSelector): _*)
                        )
                      )
                    case NamespaceSelector(namespaceSelector) =>
                      SNetworkPolicy.Peer(
                        namespaceSelector = Some(
                          LabelSelector(subinterpreter.expressions(namespaceSelector): _*)
                        )
                      )
                    case IPBlock(cidr) =>
                      SNetworkPolicy.Peer(
                        ipBlock = Some(
                          subinterpreter.ipBlocks(cidr)
                        )
                      )
                  },
                  ports = subinterpreter.ports(protocols)
                ) :: Nil
            },
            egress = obj.egress.flatMap {
              case DenyEgressRule => Nil
              case AllowEgressRule(protocols) =>
                SNetworkPolicy.EgressRule(
                  ports = subinterpreter.ports(protocols)
                ) :: Nil
              case EgressRule(to, protocols) =>
                SNetworkPolicy.EgressRule(
                  to = to.map {
                    case PodSelector(podSelector) =>
                      SNetworkPolicy.Peer(
                        podSelector = Some(
                          LabelSelector(subinterpreter.expressions(podSelector): _*)
                        )
                      )
                    case NamespaceSelector(namespaceSelector) =>
                      SNetworkPolicy.Peer(
                        namespaceSelector = Some(
                          LabelSelector(subinterpreter.expressions(namespaceSelector): _*)
                        )
                      )
                    case IPBlock(cidr) =>
                      SNetworkPolicy.Peer(
                        ipBlock = Some(
                          subinterpreter.ipBlocks(cidr)
                        )
                      )
                  },
                  ports = subinterpreter.ports(protocols)
                ) :: Nil
            },
            policyTypes = (obj.ingress.nonEmpty, obj.egress.nonEmpty) match {
              case (true, false)  => List("Ingress")
              case (false, true)  => List("Egress")
              case (true, true)   => List("Ingress", "Egress")
              case (false, false) => List()
            }
          )
        )
      )

  protected val rulePeerInterpreter: RulePeer => SNetworkPolicy.Peer = {
    case PodSelector(podSelector) =>
      SNetworkPolicy.Peer(
        podSelector = Some(
          LabelSelector(subinterpreter.expressions(podSelector): _*)
        )
      )
    case NamespaceSelector(namespaceSelector) =>
      SNetworkPolicy.Peer(
        namespaceSelector = Some(
          LabelSelector(subinterpreter.expressions(namespaceSelector): _*)
        )
      )
    case IPBlock(cidr) =>
      SNetworkPolicy.Peer(
        ipBlock = Some(
          subinterpreter.ipBlocks(cidr)
        )
      )
  }
}
