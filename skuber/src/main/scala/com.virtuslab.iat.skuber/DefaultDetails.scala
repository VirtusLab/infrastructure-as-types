package com.virtuslab.iat.skuber

import skuber.apps.v1.Deployment
import skuber.{ Container, Resource, Service }

trait DefaultDetails {
  import com.softwaremill.quicklens._

  def resourceRequirements(r: Resource.Requirements): ((Service, Deployment)) => (Service, Deployment) =
    resourceRequirements(_ => true, r)

  def resourceRequirements(
      filter: Container => Boolean,
      r: Resource.Requirements
    ): ((Service, Deployment)) => (Service, Deployment) =
    ((svc: Service, dpl: Deployment) => {
      (svc,
       dpl
         .modify(_.spec.each.template.spec.each.containers.eachWhere(filter).resources)
         .setTo(Some(r)))
    }).tupled

  def replicas(r: Int): ((Service, Deployment)) => (Service, Deployment) =
    ((svc: Service, dpl: Deployment) => {
      import com.softwaremill.quicklens._
      (svc,
       dpl
         .modify(_.spec.each.replicas.each)
         .setTo(r))
    }).tupled

  def serviceType(t: Service.Type.Value): ((Service, Deployment)) => (Service, Deployment) =
    ((svc: Service, dpl: Deployment) => {
      (svc
         .modify(_.spec.each._type)
         .setTo(t),
       dpl)
    }).tupled

  def annotations(a: Map[String, String]): ((Service, Deployment)) => (Service, Deployment) =
    ((svc: Service, dpl: Deployment) => (annotations(svc)(a), annotations(dpl)(a))).tupled

  def annotations(obj: Service)(a: Map[String, String]): Service =
    obj.modify(_.metadata.annotations).using(_ ++ a)

  def annotations(obj: Deployment)(a: Map[String, String]): Deployment =
    obj.modify(_.metadata.annotations).using(_ ++ a)

}
