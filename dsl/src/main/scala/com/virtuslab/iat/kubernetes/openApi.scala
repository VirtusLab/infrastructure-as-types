package com.virtuslab.iat.kubernetes

import com.virtuslab.iat.core.Transformable.Transformer
import com.virtuslab.iat.core._
import com.virtuslab.iat.dsl.{ Application, Configuration, Gateway, Label, Namespace, Secret }
import com.virtuslab.iat.json.json4s.JValueMetadataExtractor

object openApi {
  import com.virtuslab.iat.json.json4s.JValueTransformable
  import com.virtuslab.iat.json.playjson.PlayJsonTransformable
  import com.virtuslab.kubernetes.client.openapi.model

  object json4s extends JValueTransformable {
    import org.json4s.JValue
    object Interpreter extends InterpreterDerivation[Namespace, JValue]
    object MetaExtractor extends JValueMetadataExtractor
  }
  object playjason extends PlayJsonTransformable {
    import play.api.libs.json.JsValue
    object Interpreter extends InterpreterDerivation[Namespace, JsValue]
  }

  // TODO circe

  import Label.ops._
  import Secret.ops._

  implicit def namespaceInterpreter[R](
      implicit
      t: Transformer[model.Namespace, R]
    ): Interpreter[Namespace, Namespace, R] =
    (obj: Namespace, unused: Namespace) => {
      val namespace = model.Namespace(
        apiVersion = Some("v1"),
        kind = Some("Namespace"),
        metadata = Some(
          model.ObjectMeta(
            name = Some(obj.name),
            labels = Some(obj.labels.asMap)
          )
        )
      )

      Support(namespace) :: Nil
    }

  implicit def configurationInterpreter[R](
      implicit
      t: Transformer[model.ConfigMap, R]
    ): Interpreter[Configuration, Namespace, R] = (obj: Configuration, ns: Namespace) => {
    val config = model.ConfigMap(
      apiVersion = Some("v1"),
      kind = Some("ConfigMap"),
      metadata = Some(
        model.ObjectMeta(
          name = Some(obj.name),
          namespace = Some(ns.name),
          labels = Some(obj.labels.asMap)
        )
      ),
      data = Some(obj.data)
    )

    Support(config) :: Nil
  }

  implicit def secretInterpreter[R](implicit t: Transformer[model.Secret, R]): Interpreter[Secret, Namespace, R] =
    (obj: Secret, ns: Namespace) => {
      val secret = model.Secret(
        apiVersion = Some("v1"),
        kind = Some("Secret"),
        metadata = Some(
          model.ObjectMeta(
            name = Some(obj.name),
            namespace = Some(ns.name),
            labels = Some(obj.labels.asMap)
          )
        ),
        data = Some(obj.data.view.mapValues(base64encode).toMap)
      )

      Support(secret) :: Nil
    }

  implicit def applicationInterpreter[R](
      implicit
      t1: Transformer[model.Service, R],
      t2: Transformer[model.Deployment, R]
    ): Interpreter[Application, Namespace, R] = (obj: Application, ns: Namespace) => {
    val meta = model.ObjectMeta(
      name = Some(obj.name),
      namespace = Some(ns.name),
      labels = Some(obj.labels.asMap)
    )

    val service = model.Service(
      apiVersion = Some("v1"),
      kind = Some("Service"),
      metadata = Some(meta),
      spec = Some(model.ServiceSpec())
    )

    val deployment = model.Deployment(
      apiVersion = Some("apps/v1"),
      kind = Some("Deployment"),
      metadata = Some(meta),
      spec = Some(
        model.DeploymentSpec(
          template = model.PodTemplateSpec(
            spec = Some(
              model.PodSpec(containers = Seq(model.Container(name = obj.name)))
            )
          )
        )
      )
    )

    Support(service) :: Support(deployment) :: Nil
  }

  implicit def gatewayInterpreter[R](implicit t: Transformer[model.Ingress, R]): Interpreter[Gateway, Namespace, R] =
    (obj: Gateway, ns: Namespace) => {
      val ingress = model.Ingress(
        apiVersion = Some("networking.k8s.io/v1beta1"),
        kind = Some("Ingress"),
        metadata = Some(
          model.ObjectMeta(
            name = Some(obj.name),
            namespace = Some(ns.name),
            labels = Some(obj.labels.asMap)
          )
        )
      )
      Support(ingress) :: Nil
    }

  implicit def labelInterpreter[R](implicit t: Transformer[(String, String), R]): Interpreter[Label, Nothing, R] =
    (l: Label, unused: Nothing) => {
      Support(l.asTuple) :: Nil
    }
}
