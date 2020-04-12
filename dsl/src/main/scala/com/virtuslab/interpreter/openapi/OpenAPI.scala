package com.virtuslab.interpreter.openapi

import com.virtuslab.dsl.{ Definition, DistributedSystem, Labeled, Namespace, RootDefinition }
import com.virtuslab.interpreter.{ Context, Interpreter, RootInterpreter }
import com.virtuslab.kubernetes.client.openapi.model
import com.virtuslab.kubernetes.client.openapi.core.ApiModel

object OpenAPI {

  class OpenAPIContext extends Context {
    override type Ret = ApiModel // FIXME ?
  }

  implicit val context: OpenAPIContext = new OpenAPIContext

  implicit val systemInterpreter: RootInterpreter[OpenAPIContext, DistributedSystem, Namespace] =
    (_: RootDefinition[OpenAPIContext, DistributedSystem, Namespace]) => Seq()

  implicit val namespaceInterpreter: Interpreter[OpenAPIContext, DistributedSystem, Namespace, Labeled] =
    (namespace: Definition[OpenAPIContext, DistributedSystem, Namespace, Labeled]) =>
      Seq(
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
      )

}
