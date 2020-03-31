package com.virtuslab.dsl.interpreter

import com.virtuslab.dsl.Namespace.NamespaceDefinition
import skuber.ObjectMeta

trait NamespaceInterpreter {
  def apply(namespace: NamespaceDefinition): skuber.Namespace = {
    skuber.Namespace.from(
      ObjectMeta(
        name = namespace.name,
        labels = namespace.labels.toMap
      )
    )
  }
}

object NamespaceInterpreter extends NamespaceInterpreter
