package com.virtuslab.interpreter.openapi

import com.virtuslab.dsl.{ Definition, Namespace }
import com.virtuslab.interpreter.{ Context, Interpreter }
import com.virtuslab.kubernetes.client.openapi.model

object OpenAPI {

  class OpenAPIContext extends Context {
    override type Ret = Any // FIXME ?
  }

  implicit val context: OpenAPIContext = new OpenAPIContext

  implicit val namespaceInterpreter: Interpreter[OpenAPIContext, Namespace] =
    (namespace: Definition[OpenAPIContext, Namespace]) =>
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
