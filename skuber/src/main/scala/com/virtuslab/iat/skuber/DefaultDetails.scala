package com.virtuslab.iat.skuber

import skuber.apps.v1.Deployment
import skuber.{ Container, Resource, Service }

trait DefaultDetails {
  import com.softwaremill.quicklens._

  def resourceRequirements(r: Resource.Requirements): Deployment => Deployment =
    resourceRequirements(_ => true, r)

  def resourceRequirements(filter: Container => Boolean, r: Resource.Requirements): Deployment => Deployment =
    (dpl: Deployment) =>
      dpl
        .modify(_.spec.each.template.spec.each.containers.eachWhere(filter).resources)
        .setTo(Some(r))

  def replicas(r: Int): Deployment => Deployment =
    (dpl: Deployment) =>
      dpl
        .modify(_.spec.each.replicas.each)
        .setTo(r)

  def serviceType(t: Service.Type.Value): Service => Service =
    (svc: Service) =>
      svc
        .modify(_.spec.each._type)
        .setTo(t)
}
