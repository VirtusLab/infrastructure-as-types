package com.virtuslab.interpreter.openapi

import com.virtuslab.dsl._
import com.virtuslab.interpreter.{ Context, Interpreter, RootInterpreter }
import com.virtuslab.kubernetes.client.openapi.core.ApiModel
import com.virtuslab.kubernetes.client.openapi.model
import com.virtuslab.materializer.openapi.Resource

object OpenAPI {

  class OpenAPIContext extends Context {
    override type T = ApiModel
    override type Ret = Resource[T]
  }

  implicit val context: OpenAPIContext = new OpenAPIContext

  implicit val systemInterpreter: RootInterpreter[OpenAPIContext, DistributedSystem, Namespace] =
    (_: RootDefinition[OpenAPIContext, DistributedSystem, Namespace]) => Seq()

  implicit val namespaceInterpreter: Interpreter[OpenAPIContext, DistributedSystem, Namespace, Labeled] =
    (namespace: Definition[OpenAPIContext, DistributedSystem, Namespace, Labeled]) =>
      Seq(
        Resource(
          model.Namespace(
            apiVersion = Some("v1"),
            kind = Some("Namespace"),
            metadata = Some(
              model.ObjectMeta(
                name = Some(namespace.obj.name),
                labels = Some(namespace.obj.labels.toMap)
              )
            )
          )
        ).weak
      )

}
