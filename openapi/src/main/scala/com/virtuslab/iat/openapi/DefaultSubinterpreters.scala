package com.virtuslab.iat.openapi

import com.virtuslab.iat.dsl.{ Label, Labeled, Named }
import com.virtuslab.iat.kubernetes.dsl.{ Application, Namespace }
import com.virtuslab.kubernetes.client.openapi.model
import com.virtuslab.kubernetes.client.openapi.model.{ Deployment, ObjectMeta, Service }

trait DefaultSubinterpreters {
  import Label.ops._

  def objectMetaInterpreter(obj: Labeled with Named, ns: Namespace): ObjectMeta = {
    model.ObjectMeta(
      name = Some(obj.name),
      namespace = Some(ns.name),
      labels = Some(obj.labels.asMap)
    )
  }

  def applicationServiceInterpreter(obj: Application, ns: Namespace): Service = {
    model.Service(
      apiVersion = Some("v1"),
      kind = Some("Service"),
      metadata = Some(objectMetaInterpreter(obj, ns)),
      spec = Some(model.ServiceSpec())
    )
  }

  def applicationDeploymentInterpreter(obj: Application, ns: Namespace): Deployment = {
    model.Deployment(
      apiVersion = Some("apps/v1"),
      kind = Some("Deployment"),
      metadata = Some(objectMetaInterpreter(obj, ns)),
      spec = Some(
        model.DeploymentSpec(
          template = model.PodTemplateSpec(
            spec = Some(
              model.PodSpec(containers = Seq(model.Container(name = obj.name)))
            )
          )
        )
      )
    )
  }
}
