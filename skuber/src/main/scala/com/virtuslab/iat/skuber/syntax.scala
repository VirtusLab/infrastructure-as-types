package com.virtuslab.iat.skuber

import com.virtuslab.iat.scala.{ FunctionOps, TupleOps }
import skuber.ext.Ingress

object dsl {
  import com.virtuslab.iat.kubernetes.dsl.{ Configuration, Secret }
  import com.virtuslab.iat.kubernetes.dsl.Mountable.MountSource
  import skuber.Volume.{ ConfigMapVolumeSource, Secret => SecretVolumeSource }

  implicit val secretMountSource: MountSource[Secret, SecretVolumeSource] =
    (obj: Secret) => SecretVolumeSource(secretName = obj.name)

  implicit val configurationMountSource: MountSource[Configuration, ConfigMapVolumeSource] =
    (obj: Configuration) => ConfigMapVolumeSource(name = obj.name)
}

object deployment extends UpsertOps with DefaultInterpreters with DefaultDeinterpreters with InterpreterOps with TupleOps

object playjson extends PlayJsonProcessors with DefaultInterpreters with InterpreterOps with TupleOps with ApiOps

object interpreter extends DefaultInterpreters
object subinterpreter extends DefaultSubinterpreters
object details extends DefaultDetails with FunctionOps
object deinterpreter extends DefaultDeinterpreters

import com.virtuslab.iat.core
import skuber.ObjectResource

object experimental extends core.InterpreterOps[ObjectResource] {
  import com.virtuslab.iat.kubernetes.dsl.Namespace
  import com.virtuslab.iat.kubernetes.dsl.Application
  import com.virtuslab.iat.kubernetes.dsl.Configuration
  import com.virtuslab.iat.kubernetes.dsl.Gateway
  import skuber.Service
  import skuber.ConfigMap
  import skuber.apps.v1.Deployment

  implicit class NamespacedApplicationInterpreterOps(val arguments: (Application, Namespace))
    extends Contextualized2[Application, Namespace, Service, Deployment]
  implicit class NamespacedConfigurationInterpreterOps(val arguments: (Configuration, Namespace))
    extends Contextualized1[Configuration, Namespace, ConfigMap]
  implicit class NamespacedGatewayInterpreterOps(val arguments: (Gateway, Namespace))
    extends Contextualized1[Gateway, Namespace, Ingress]
}
