package com.virtuslab.iat.skuber

import _root_.skuber.apps.v1.Deployment
import _root_.skuber.ext.Ingress
import _root_.skuber.networking.NetworkPolicy
import _root_.skuber.networking.NetworkPolicy.{EgressRule, IngressRule, Peer, Spec}
import _root_.skuber.{ConfigMap, LabelSelector, ObjectMeta, Service, Namespace => SNamespace, Secret => SSecret}
import com.virtuslab.iat.dsl.{Label, _}
import com.virtuslab.iat.kubernetes.dsl._

trait DefaultInterpreters {
  import Label.ops._

  implicit val namespaceInterpreter: Namespace => SNamespace =
    (ns: Namespace) =>
      SNamespace.from(
        ObjectMeta(
          name = ns.name,
          labels = ns.labels.asMap
        )
      )

  implicit val applicationInterpreter: (Application, Namespace) => (Service, Deployment) =
    (obj: Application, ns: Namespace) => {
      val service = subinterpreter.serviceInterpreter(obj, ns)
      val deployment = subinterpreter.deploymentInterpreter(obj, ns)
      (service, deployment)
    }

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

  implicit val gatewayInterpreter: (Gateway, Namespace) => Ingress = (obj: Gateway, ns: Namespace) => {
    Ingress(
      apiVersion = "networking.k8s.io/v1beta1", // Skuber uses wrong api version
      metadata = subinterpreter.objectMetaInterpreter(obj, ns),
      spec = obj.protocols match {
        case Protocols.Any => None
        case Protocols.Selected(layers) =>
          Some(
            Ingress.Spec(
              rules = layers.map {
                case Protocol.AnyLayers => Ingress.Rule(host = None, http = Ingress.HttpRule())
                case Protocol.SomeLayers(http: HTTP, tcp: TCP, _) =>
                  Ingress.Rule(
                    host = http.host.get,
                    http = Ingress.HttpRule(
                      paths = List(
                        Ingress.Path(
                          http.path.get.getOrElse("/"),
                          Ingress.Backend(
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

  implicit val connectionInterpreter: (Connection, Namespace) => NetworkPolicy = (obj: Connection, ns: Namespace) => {
    NetworkPolicy(
      metadata = subinterpreter.objectMetaInterpreter(obj, ns),
      spec = Some(
        Spec(
          podSelector = obj.resourceSelector match {
            // NoSelector and AllowSelector are interchangeable here
            case s: Selector =>
              LabelSelector(
                subinterpreter.expressions(s.expressions): _*
              )
          },
          ingress = obj.ingress match {
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
                          subinterpreter.expressions(s.expressions): _*
                        )
                      ),
                      namespaceSelector = None,
                      ipBlock = None
                    )
                    // TODO multiple "Peers", combined with logical OR
                  ),
                  ports = subinterpreter.ports(s.protocols)
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
                          subinterpreter.expressions(s.expressions): _*
                        )
                      ),
                      ipBlock = None
                    )
                    // TODO multiple "Peers", combined with logical OR
                  ),
                  ports = subinterpreter.ports(s.protocols)
                )
                // TODO multiple "Rules", combined with logical OR
              )
            case s: SelectedIPs =>
              List(
                IngressRule(
                  from = subinterpreter.ipBlocks(s),
                  ports = subinterpreter.ports(s.protocols)
                )
              )
            case s: SelectedIPsAndPorts =>
              List(
                IngressRule(
                  from = subinterpreter.ipBlocks(s),
                  ports = subinterpreter.ports(s.protocols)
                )
                // TODO multiple "Rules", combined with logical OR
              )
          },
          egress = obj.egress match {
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
                          subinterpreter.expressions(s.expressions): _*
                        )
                      ),
                      namespaceSelector = None,
                      ipBlock = None
                    )
                    // TODO multiple "Peers", combined with logical OR
                  ),
                  ports = subinterpreter.ports(s.protocols)
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
                          subinterpreter.expressions(s.expressions): _*
                        )
                      ),
                      ipBlock = None
                    )
                    // TODO multiple "Peers", combined with logical OR
                  ),
                  ports = subinterpreter.ports(s.protocols)
                )
                // TODO multiple "Rules", combined with logical OR
              )
            case s: SelectedIPs =>
              List(
                EgressRule(
                  to = subinterpreter.ipBlocks(s),
                  ports = subinterpreter.ports(s.protocols)
                )
                // TODO multiple "Rules", combined with logical OR
              )
            case s: SelectedIPsAndPorts =>
              List(
                EgressRule(
                  to = subinterpreter.ipBlocks(s),
                  ports = subinterpreter.ports(s.protocols)
                )
                // TODO multiple "Rules", combined with logical OR
              )
          },
          policyTypes = (obj.ingress, obj.egress) match {
            case (NoSelector, NoSelector) => List()
            case (_, NoSelector)          => List("Ingress")
            case (NoSelector, _)          => List("Egress")
            case (_, _)                   => List("Ingress", "Egress")
          }
        )
      )
    )
  }
}
