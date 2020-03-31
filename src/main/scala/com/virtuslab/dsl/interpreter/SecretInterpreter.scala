package com.virtuslab.dsl.interpreter

import com.virtuslab.dsl.Secret.SecretDefinition
import skuber.{ ObjectMeta, Secret }

trait SecretInterpreter {
  def apply(secret: SecretDefinition): Secret = {
    Secret(metadata = ObjectMeta(
             name = ???,
             namespace = ???,
             labels = ???
           ),
           data = ???)
  }
}

object SecretInterpreter extends SecretInterpreter
