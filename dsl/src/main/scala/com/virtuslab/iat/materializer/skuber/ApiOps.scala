package com.virtuslab.iat.materializer.skuber

import com.virtuslab.iat.kubernetes.skuber.Base
import play.api.libs.json.Format
import skuber.api.client.LoggingContext
import skuber.{ K8SRequestContext, ResourceDefinition }

import scala.concurrent.ExecutionContext

trait ApiOps {

  import com.virtuslab.iat.scala.ops._

  implicit class BaseOps[A <: Base: Format: ResourceDefinition](a: A) {
    def upsert(
        implicit
        executor: ExecutionContext,
        client: K8SRequestContext,
        lc: LoggingContext
      ): Either[Throwable, A] = SimpleDeployer.upsert(a)
  }

  implicit class BaseOps2[A1 <: Base: Format: ResourceDefinition, A2 <: Base: Format: ResourceDefinition](t: (A1, A2)) {
    def upsert(
        implicit
        executor: ExecutionContext,
        client: K8SRequestContext,
        lc: LoggingContext
      ): (Either[Throwable, A1], Either[Throwable, A2]) = t.map(_.upsert, _.upsert)
  }
}
