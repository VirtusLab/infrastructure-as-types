package com.virtuslab.dsl.interpreter

import com.virtuslab.dsl.Configuration.DefinedConfiguration
import skuber.{ ConfigMap, ObjectMeta }

class ConfigurationInterpreter() {
  def apply(configuration: DefinedConfiguration): ConfigMap = {
    ConfigMap(
      metadata = ObjectMeta(name = configuration.name, namespace = configuration.namespace.name),
      data = configuration.data
    )
  }
}
