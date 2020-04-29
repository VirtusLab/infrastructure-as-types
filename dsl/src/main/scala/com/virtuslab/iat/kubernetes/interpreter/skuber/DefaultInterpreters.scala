package com.virtuslab.iat.kubernetes.interpreter.skuber

import _root_.skuber.apps.v1.Deployment
import _root_.skuber.{ ObjectMeta, Service, Namespace => SNamespace }
import com.virtuslab.iat.dsl.Label
import com.virtuslab.iat.dsl.kubernetes.{ Application, Namespace }
import com.virtuslab.iat.kubernetes.skuber.subinterpreter

trait DefaultInterpreters {
  import Label.ops._

  implicit val namespaceInterpreter2: Namespace => SNamespace =
    (ns: Namespace) =>
      SNamespace.from(
        ObjectMeta(
          name = ns.name,
          labels = ns.labels.asMap
        )
      )

  implicit val applicationInterpreter2: (Application, Namespace) => (Service, Deployment) =
    (obj: Application, ns: Namespace) => {
      val service = subinterpreter.serviceInterpreter(obj, ns)
      val deployment = subinterpreter.deploymentInterpreter(obj, ns)
      (service, deployment)
    }
}
