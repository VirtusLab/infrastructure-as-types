package com.virtuslab.iat.openapi

import com.virtuslab.iat.core
import com.virtuslab.iat.dsl.{ Interpretable, Peer }
import com.virtuslab.iat.kubernetes.dsl._
import com.virtuslab.kubernetes.client.openapi.core.ApiModel
import com.virtuslab.kubernetes.client.openapi.model

trait InterpreterOps extends core.InterpreterOps[ApiModel] {
  implicit class NamespaceInterpretationOps(val obj: Interpretable[Namespace])
    extends RootInterpreterOps1[Namespace, model.Namespace]
  implicit class ApplicationInterpreterOps(val obj: Interpretable[Application])
    extends InterpreterOps2[Application, Namespace, model.Service, model.Deployment]
  implicit class GatewayInterpretationOps(val obj: Gateway) extends InterpreterOps1[Gateway, Namespace, model.Ingress]
  implicit class ConfigurationInterpretationOps(val obj: Configuration)
    extends InterpreterOps1[Configuration, Namespace, model.ConfigMap]
  implicit class SecretInterpretationOps(val obj: Secret) extends InterpreterOps1[Secret, Namespace, model.Secret]
  implicit class NetworkPolicyInterpretationOps[A <: Peer[A], B <: Peer[B]](val obj: NetworkPolicy[A, B])
    extends InterpreterOps1[NetworkPolicy[A, B], Namespace, model.NetworkPolicy]
}
