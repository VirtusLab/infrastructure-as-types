package com.virtuslab.iat.skuber

import _root_.skuber.{ ConfigMap, ObjectResource, Service, Namespace => SNamespace, Secret => SSecret }
import com.virtuslab.iat.core
import com.virtuslab.iat.kubernetes.dsl._
import skuber.apps.v1.Deployment
import skuber.ext.{ Ingress => SIngress }
import skuber.networking.{ NetworkPolicy => SNetworkPolicy }

trait InterpreterOps extends core.InterpreterOps[ObjectResource] {
  implicit class NamespaceInterpretationOps(val obj: Namespace) extends RootInterpreterOps1[Namespace, SNamespace]
  implicit class ApplicationInterpreterOps(val obj: Application)
    extends InterpreterOps2[Application, Namespace, Service, Deployment]
  implicit class GatewayInterpretationOps(val obj: Gateway) extends InterpreterOps1[Gateway, Namespace, SIngress]
  implicit class ConfigurationInterpretationOps(val obj: Configuration)
    extends InterpreterOps1[Configuration, Namespace, ConfigMap]
  implicit class SecretInterpretationOps(val obj: Secret) extends InterpreterOps1[Secret, Namespace, SSecret]
  implicit class NetworkPolicyInterpretationOps(val obj: NetworkPolicy)
    extends InterpreterOps1[NetworkPolicy, Namespace, SNetworkPolicy]
}
