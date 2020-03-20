package com.virtuslab.dsl.interpreter

import com.virtuslab.dsl.{ Connection, DistributedSystem }
import skuber.ObjectMeta
import skuber.networking.NetworkPolicy

class ConnectionInterpreter(system: DistributedSystem) {

  def apply(connection: Connection[_, _, _]): NetworkPolicy = {
    NetworkPolicy(
      metadata = ObjectMeta(
        name = connection.name,
        namespace = connection.namespace.name,
        labels = connection.labels.toMap
      ),
      spec = None
    )
  }

}
