package com.virtuslab.dsl.v2

import com.virtuslab.json.json4s.jackson.JsonMethods
import org.json4s.Formats
import org.json4s.JsonAST.JValue

object openApi {
  import com.virtuslab.kubernetes.client.openapi.model

  object json4sSerializers {
    implicit val formats: Formats = JsonMethods.defaultFormats

    private class JValueTransformer[A] extends Transformer[A, JValue] {
      override def apply(obj: A): Seq[JValue] = Seq(JsonMethods.asJValue(obj))
    }

    implicit val configMapTransformer: Transformer[model.ConfigMap, JValue] = new JValueTransformer[model.ConfigMap]
    implicit val deploymentTransformer: Transformer[model.Deployment, JValue] = new JValueTransformer[model.Deployment]
    implicit val namespaceTransformer: Transformer[model.Namespace, JValue] = new JValueTransformer[model.Namespace]
    implicit val secretTransformer: Transformer[model.Secret, JValue] = new JValueTransformer[model.Secret]
    implicit val serviceTransformer: Transformer[model.Service, JValue] = new JValueTransformer[model.Service]
  }

//  object playJsonSerializers {
//    import play.api.libs.json.Json
//
//    implicit val configMapTransformer: Transformer[model.ConfigMap, JsObject] = (obj: model.ConfigMap) =>
//      Json.writes[model.ConfigMap].writes(obj)
//    implicit val deploymentTransformer: Transformer[model.Deployment, JsObject] = (obj: model.Deployment) =>
//      Json.writes[model.Deployment].writes(obj)
//    implicit val namespaceTransformer: Transformer[model.Namespace, JsObject] = (obj: model.Namespace) =>
//      Json.writes[model.Namespace].writes(obj)
//    implicit val secretTransformer: Transformer[model.Secret, JsObject] = (obj: model.Secret) =>
//      Json.writes[model.Secret].writes(obj)
//    implicit val serviceTransformer: Transformer[model.Service, JsObject] = (obj: model.Service) =>
//      Json.writes[model.Service].writes(obj)
//  }

  object interpreters {
    implicit val namespaceInterpreter: Interpreter[Namespace] = new Interpreter[Namespace] {
      override type R = model.Namespace

      override def interpret(obj: Namespace, ns: Namespace): model.Namespace = {
        model.Namespace(
          metadata = Some(
            model.ObjectMeta(
              name = Some(obj.name)
            )
          )
        )
      }
    }

    implicit val configurationInterpreter: Interpreter[Configuration] = new Interpreter[Configuration] {
      override type R = model.ConfigMap

      override def interpret(obj: Configuration, ns: Namespace): model.ConfigMap = {
        val meta = model.ObjectMeta(
          name = Some(obj.name),
          namespace = Some(ns.name)
        )

        model.ConfigMap(metadata = Some(meta), data = Some(obj.data))
      }
    }

    implicit val secretInterpreter: Interpreter[Secret] = new Interpreter[Secret] {
      override type R = model.Secret

      override def interpret(obj: Secret, ns: Namespace): model.Secret = {
        val meta = model.ObjectMeta(name = Some(obj.name), namespace = Some(ns.name))
        val data = obj.data.view.mapValues(_.getBytes).toMap

        model.Secret(metadata = Some(meta), data = Some(data))
      }
    }

    implicit val applicationInterpreter: Interpreter[Application] = new Interpreter[Application] {
      override type R = (model.Service, model.Deployment)

      override def interpret(obj: Application, ns: Namespace): (model.Service, model.Deployment) = {
        val meta = model.ObjectMeta(name = Some(obj.name), namespace = Some(ns.name))

        val container = model.Container(name = obj.name)
        val podSpec = model.PodSpec(containers = Seq(container))
        val podTemplateSpec = model.PodTemplateSpec(spec = Some(podSpec))
        val deploymentSpec = model.DeploymentSpec(template = podTemplateSpec)
        val deployment = model.Deployment(metadata = Some(meta), spec = Some(deploymentSpec))

        val serviceSpec = model.ServiceSpec()
        val service = model.Service(metadata = Some(meta), spec = Some(serviceSpec))

        (service, deployment)
      }
    }
  }

}
