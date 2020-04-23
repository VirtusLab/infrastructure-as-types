package com.virtuslab.iat.materialization.skuber

import play.api.libs.json.Format
import skuber.api.client.LoggingContext
import skuber.{ K8SRequestContext, ObjectResource, ResourceDefinition }

import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContext, Future }

case class SimpleDeployer(client: K8SRequestContext) {

  def createOrUpdate[T <: ObjectResource](
      o: T
    )(implicit
      executor: ExecutionContext,
      format: Format[T],
      definition: ResourceDefinition[T],
      lc: LoggingContext
    ): T = {
    val future = _createOrUpdate(o)
    val result = Await.result(future, 1.minute)
    println(s"Successfully created or updated '$result' on Kubernetes cluster")
    result
  }

  private def _createOrUpdate[T <: ObjectResource](
      o: T
    )(implicit
      executor: ExecutionContext,
      format: Format[T],
      definition: ResourceDefinition[T],
      lc: LoggingContext
    ): Future[T] = {
    import skuber._

    client.create(o)(format, definition, lc) recoverWith {
      case ex: K8SException if ex.status.code.contains(409) =>
        client.update(o)(format, definition, lc)
    }
  }
}
