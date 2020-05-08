package com.virtuslab.iat.openapi

import com.virtuslab.iat
import com.virtuslab.iat.kubernetes.dsl._
import com.virtuslab.kubernetes.client.custom.B64Encoded
import com.virtuslab.kubernetes.client.openapi.model

trait DefaultInterpreters {
  import iat.dsl.Label.ops._
  import iat.scala.ops._

  implicit val namespaceInterpreter: Namespace => model.Namespace =
    (obj: Namespace) =>
      model.Namespace(
        apiVersion = Some("v1"), // FIXME: should be in model
        kind = Some("Namespace"), // FIXME: should be in model
        metadata = Some(
          model.ObjectMeta(
            name = Some(obj.name),
            labels = Some(obj.labels.asMap)
          )
        )
      )

  implicit val configurationInterpreter: (Configuration, Namespace) => model.ConfigMap =
    (obj: Configuration, ns: Namespace) =>
      model.ConfigMap(
        apiVersion = Some("v1"), // FIXME: should be in model
        kind = Some("ConfigMap"), // FIXME: should be in model
        metadata = Some(subinterpreter.objectMetaInterpreter(obj, ns)),
        data = Some(obj.data)
      )

  implicit val secretInterpreter: (Secret, Namespace) => model.Secret =
    (obj: Secret, ns: Namespace) =>
      model.Secret(
        apiVersion = Some("v1"), // FIXME: should be in model
        kind = Some("Secret"), // FIXME: should be in model
        metadata = Some(subinterpreter.objectMetaInterpreter(obj, ns)),
        data = Some(obj.data.view.mapValues(B64Encoded.apply).toMap)
      )

  implicit val applicationInterpreter: (Application, Namespace) => (model.Service, model.Deployment) =
    (subinterpreter.applicationServiceInterpreter _)
      .merge(subinterpreter.applicationDeploymentInterpreter)

  implicit val gatewayInterpreter: (Gateway, Namespace) => model.Ingress =
    (obj: Gateway, ns: Namespace) =>
      model.Ingress(
        apiVersion = Some("networking.k8s.io/v1beta1"), // FIXME: should be in model
        kind = Some("Ingress"), // FIXME: should be in model
        metadata = Some(subinterpreter.objectMetaInterpreter(obj, ns))
      )
}
