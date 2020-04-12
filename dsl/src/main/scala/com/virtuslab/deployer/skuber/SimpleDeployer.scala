package com.virtuslab.deployer.skuber

import com.virtuslab.exporter.skuber.Resource
import com.virtuslab.interpreter.SystemInterpreter
import com.virtuslab.interpreter.skuber.Skuber.SkuberContext
import skuber.api.client.LoggingContext
import skuber.{ K8SRequestContext, ObjectResource }

import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContext, Future }

object SimpleDeployer {
  def createOrUpdate(
      client: K8SRequestContext,
      interpreter: SystemInterpreter[SkuberContext]
    )(implicit
      executor: ExecutionContext
    ): Unit = {
    val resources = interpreter.resources
    resources.map(createOrUpdate(client))
  }

  def createOrUpdate(client: K8SRequestContext)(implicit executor: ExecutionContext): Resource[ObjectResource] => ObjectResource =
    (resource: Resource[ObjectResource]) => {
      val future = createOrUpdate(client, resource)
      val result = Await.result(future, 1.minute)
      println(s"Successfully created '$result' on Kubernetes cluster")
      result
    }

  def createOrUpdate(
      client: K8SRequestContext,
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
