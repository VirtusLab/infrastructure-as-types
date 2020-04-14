package com.virtuslab.interpreter.openapi

import com.virtuslab.dsl._
import com.virtuslab.interpreter.{ Context, Interpreter, RootInterpreter }
import com.virtuslab.kubernetes.client.openapi.core.ApiModel
import com.virtuslab.kubernetes.client.openapi.model
import com.virtuslab.materializer.openapi.{ Metadata, Resource }

object OpenAPI {

  class OpenAPIContext extends Context {
    override type Meta = Metadata
    override type Base = ApiModel
    override type Interpretation = Resource[Base]
  }

  implicit val context: OpenAPIContext = new OpenAPIContext

  implicit val systemInterpreter: RootInterpreter[OpenAPIContext, DistributedSystem, Namespace] =
    (_: RootDefinition[OpenAPIContext, DistributedSystem, Namespace]) => Seq()

  implicit val namespaceInterpreter: Interpreter[OpenAPIContext, DistributedSystem, Namespace, Labeled] =
    (namespace: Definition[OpenAPIContext, DistributedSystem, Namespace, Labeled]) => {
      val meta = Metadata("v1", "Namespace", "", namespace.obj.name)
      val ns = model.Namespace(
        apiVersion = Some(meta.apiVersion),
        kind = Some(meta.kind),
        metadata = Some(
          model.ObjectMeta(
            name = Some(namespace.obj.name),
            labels = Some(namespace.obj.labels.toMap)
          )
        )
      )
      Seq(Resource(meta, ns).weak)
    }
}
