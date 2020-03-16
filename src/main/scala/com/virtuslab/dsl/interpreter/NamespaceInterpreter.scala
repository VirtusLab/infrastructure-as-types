package com.virtuslab.dsl.interpreter

import com.virtuslab.dsl.Namespace.DefinedNamespace
import skuber.ObjectMeta

class NamespaceInterpreter() {
  def apply(namespace: DefinedNamespace): skuber.Namespace = {
    skuber.Namespace.from(ObjectMeta(name = namespace.name))
  }
}
