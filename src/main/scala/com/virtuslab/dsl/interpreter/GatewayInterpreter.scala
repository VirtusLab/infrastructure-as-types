package com.virtuslab.dsl.interpreter

import com.virtuslab.dsl.Gateway.GatewayDefinition
import com.virtuslab.dsl.{ HTTP, Protocol, Protocols, TCP }
import skuber.ObjectMeta
import skuber.ext.Ingress

class GatewayInterpreter {
  def apply(gateway: GatewayDefinition): Ingress = {
    Ingress(
      apiVersion = "networking.k8s.io/v1beta1", // Skuber uses wrong api version
      metadata = ObjectMeta(
        name = gateway.name,
        namespace = gateway.namespace.name,
        labels = gateway.labels.toMap
      ),
      spec = gateway.protocols match {
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
                            servicePort = tcp.port.numberOrName.left.get // FIXME
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
}
