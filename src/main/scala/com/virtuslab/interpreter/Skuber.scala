package com.virtuslab.interpreter

import com.virtuslab.dsl.{ Configuration, Definition }
import com.virtuslab.exporter.skuber.Resource
import skuber.json.format._
import skuber.{ ConfigMap, ObjectMeta }

object Skuber {

  class SkuberContext extends Context {
    override type Ret[A] = Seq[Resource[A]]
  }

  implicit val context: SkuberContext = new SkuberContext

  implicit val configurationInterpreter: Interpreter[SkuberContext, Configuration] =
    (cfg: Definition[SkuberContext, Configuration]) => {
      Seq(
        Resource(
          ConfigMap(
            metadata = ObjectMeta(
              name = cfg.obj.name,
              namespace = cfg.namespace.name,
              labels = cfg.obj.labels.toMap
            ),
            data = cfg.obj.data
          )
        )
      )
    }

}
