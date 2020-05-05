package com.virtuslab.iat.materializer.skuber

import com.virtuslab.iat.kubernetes.skuber.Base
import play.api.libs.json.Format
import skuber.api.client.LoggingContext
import skuber.{ K8SException, K8SRequestContext, ResourceDefinition }

import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.util.Try

object SimpleDeployer {

  protected def futureUpsert[A <: Base: Format: ResourceDefinition](
      o: A
    )(implicit
      executor: ExecutionContext,
      client: K8SRequestContext,
      lc: LoggingContext
    ): Future[A] = {

    client.create(o) recoverWith {
      case ex: K8SException if ex.status.code.contains(409) =>
        client.update(o)
    }
  }

  def upsert[A <: Base: Format: ResourceDefinition](
      o: A
    )(implicit
      executor: ExecutionContext,
      client: K8SRequestContext,
      lc: LoggingContext
    ): Either[Throwable, A] = {
    Try {
      val future = futureUpsert(o)
      val result = Await.result(future, 1.minute)
      println(s"Successfully created or updated '$result' on Kubernetes cluster")
      result
    }.toEither
  }
}
