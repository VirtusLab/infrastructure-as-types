package com.virtuslab.iat.openapi

import com.virtuslab.iat.core
import com.virtuslab.iat.dsl.Interpretable
import com.virtuslab.iat.kubernetes.dsl._
import com.virtuslab.kubernetes.client.openapi.core.ApiModel
import com.virtuslab.kubernetes.client.openapi.model.{ ConfigMap, Deployment, Ingress, NetworkPolicy, Service, Namespace => MNamespace, Secret => MSecret }

trait InterpreterOps extends core.InterpreterOps[ApiModel] {
  implicit class NamespaceInterpretationOps(val obj: Interpretable[Namespace]) extends RootInterpreterOps1[Namespace, MNamespace]
  implicit class ApplicationInterpreterOps(val obj: Interpretable[Application])
    extends InterpreterOps2[Application, Namespace, Service, Deployment]
  implicit class ConnectionInterpretationOps(val obj: Connection) extends InterpreterOps1[Connection, Namespace, NetworkPolicy]
  implicit class GatewayInterpretationOps(val obj: Gateway) extends InterpreterOps1[Gateway, Namespace, Ingress]
  implicit class ConfigurationInterpretationOps(val obj: Configuration)
    extends InterpreterOps1[Configuration, Namespace, ConfigMap]
  implicit class SecretInterpretationOps(val obj: Secret) extends InterpreterOps1[Secret, Namespace, MSecret]
}
