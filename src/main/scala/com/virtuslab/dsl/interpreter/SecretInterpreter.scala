package com.virtuslab.dsl.interpreter

import java.util.Base64

import com.virtuslab.dsl.Secret.SecretDefinition
import skuber.{ ObjectMeta, Secret }

trait SecretInterpreter {
  def apply(secret: SecretDefinition): Secret = {
    Base64.getEncoder
    Secret(metadata = ObjectMeta(
             name = secret.name,
             namespace = secret.namespace.name,
             labels = secret.labels.toMap
           ),
           data = secret.data.mapValues(_.getBytes))
  }
}

object SecretInterpreter extends SecretInterpreter
