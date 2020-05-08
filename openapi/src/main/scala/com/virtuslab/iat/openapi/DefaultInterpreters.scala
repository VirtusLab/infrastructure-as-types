package com.virtuslab.iat.openapi

import com.virtuslab.iat.dsl.Label
import com.virtuslab.iat.kubernetes.dsl._
import com.virtuslab.kubernetes.client.custom.B64Encoded
import com.virtuslab.kubernetes.client.openapi.model

trait DefaultInterpreters {
  import Label.ops._

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
        metadata = Some(
          model.ObjectMeta(
            name = Some(obj.name),
            namespace = Some(ns.name),
            labels = Some(obj.labels.asMap)
          )
        ),
        data = Some(obj.data)
      )

  implicit val secretInterpreter: (Secret, Namespace) => model.Secret =
    (obj: Secret, ns: Namespace) =>
      model.Secret(
        apiVersion = Some("v1"), // FIXME: should be in model
        kind = Some("Secret"), // FIXME: should be in model
        metadata = Some(
          model.ObjectMeta(
            name = Some(obj.name),
            namespace = Some(ns.name),
            labels = Some(obj.labels.asMap)
          )
        ),
        data = Some(obj.data.view.mapValues(B64Encoded.apply).toMap)
      )

  implicit val applicationInterpreter: (Application, Namespace) => (model.Service, model.Deployment) =
    (obj: Application, ns: Namespace) => {
      val meta = model.ObjectMeta(
        name = Some(obj.name),
        namespace = Some(ns.name),
        labels = Some(obj.labels.asMap)
      )

      val service = subinterpreter.applicationServiceInterpreter(meta)
      val deployment = subinterpreter.applicationDeploymentInterpreter(meta, obj)

      (service, deployment)
    }

  implicit val gatewayInterpreter: (Gateway, Namespace) => model.Ingress =
    (obj: Gateway, ns: Namespace) =>
      model.Ingress(
        apiVersion = Some("networking.k8s.io/v1beta1"), // FIXME: should be in model
        kind = Some("Ingress"), // FIXME: should be in model
        metadata = Some(
          model.ObjectMeta(
            name = Some(obj.name),
            namespace = Some(ns.name),
            labels = Some(obj.labels.asMap)
          )
        )
      )
}
