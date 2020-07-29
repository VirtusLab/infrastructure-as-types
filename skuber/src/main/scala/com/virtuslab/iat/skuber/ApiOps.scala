package com.virtuslab.iat.skuber

import play.api.libs.json.Format
import skuber.api.client.LoggingContext
import skuber.{ K8SRequestContext, ObjectResource, ResourceDefinition }

import scala.concurrent.ExecutionContext

trait ApiOps {

  import com.virtuslab.iat.scala.ops._

  implicit class ObjectResourceOps[A <: ObjectResource](a: A) {
    def map[B](f: A => B): B = f(a)
  }

  implicit class ObjectResourceOps1[A <: ObjectResource: Format: ResourceDefinition](a: A) {
    def upsert(
        implicit
        executor: ExecutionContext,
        client: K8SRequestContext,
        lc: LoggingContext
      ): Either[Throwable, A] = SimpleDeployer.upsert(a)
  }

  implicit class ObjectResourceOps2[
      A1 <: ObjectResource: Format: ResourceDefinition,
      A2 <: ObjectResource: Format: ResourceDefinition
    ](
      t: (A1, A2)) {
    def upsert(
        implicit
        executor: ExecutionContext,
        client: K8SRequestContext,
        lc: LoggingContext
      ): (Either[Throwable, A1], Either[Throwable, A2]) = t.map(_.upsert, _.upsert)
  }
}
