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

}
