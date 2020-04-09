package com.virtuslab.interpreter

import com.virtuslab.dsl.{ Configuration, Definition }
import com.virtuslab.exporter.skuber.Resource
import skuber.json.format._
import skuber.{ ConfigMap, ObjectMeta }

object Skuber {

  class SkuberContxt extends Context {
    override type Ret[A] = Seq[Resource[A]]
  }

  implicit val context = new SkuberContxt

  implicit val configurationInterpreter = new Interpreter[SkuberContxt, Configuration] {
    override def apply(cfg: Definition[SkuberContxt, Configuration]): SkuberContxt#Ret[ConfigMap] = {
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

}
