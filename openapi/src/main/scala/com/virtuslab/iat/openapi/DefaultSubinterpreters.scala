package com.virtuslab.iat.openapi

import com.virtuslab.iat.kubernetes.dsl.Application
import com.virtuslab.kubernetes.client.openapi.model
import com.virtuslab.kubernetes.client.openapi.model.{ Deployment, ObjectMeta, Service }

trait DefaultSubinterpreters {

  def applicationServiceInterpreter(meta: model.ObjectMeta): Service = {
    model.Service(
      apiVersion = Some("v1"),
      kind = Some("Service"),
      metadata = Some(meta),
      spec = Some(model.ServiceSpec())
    )
  }

  def applicationDeploymentInterpreter(meta: ObjectMeta, obj: Application): Deployment = {
    model.Deployment(
      apiVersion = Some("apps/v1"),
      kind = Some("Deployment"),
      metadata = Some(meta),
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
