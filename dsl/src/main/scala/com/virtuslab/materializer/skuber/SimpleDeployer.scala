package com.virtuslab.materializer.skuber

import skuber.api.client.LoggingContext
import skuber.{ K8SRequestContext, ObjectResource }

import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContext, Future }

case class SimpleDeployer(client: K8SRequestContext) {

  def createOrUpdate(resource: Resource[ObjectResource])(implicit executor: ExecutionContext): ObjectResource = {
    val future = _createOrUpdate(resource)
    val result = Await.result(future, 1.minute)
    println(s"Successfully created or updated '$result' on Kubernetes cluster")
    result
  }

  private def _createOrUpdate(
      resource: Resource[ObjectResource]
    )(implicit
      executor: ExecutionContext
    ): Future[ObjectResource] = {
    import skuber._

    client.create(resource.obj)(resource.format, resource.definition, LoggingContext.lc) recoverWith {
      case ex: K8SException if ex.status.code.contains(409) =>
        client.update(resource.obj)(resource.format, resource.definition, LoggingContext.lc)
    }
  }
}
