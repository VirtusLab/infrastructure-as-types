package com.virtuslab.iat.materializer.skuber

import com.virtuslab.iat.kubernetes.skuber.deployment.{ result, MaybeResult, Processor }
import com.virtuslab.iat.kubernetes.skuber.{ Base, SResource }
import skuber.K8SRequestContext
import skuber.api.client.LoggingContext

import scala.concurrent.ExecutionContext
import scala.util.Try

trait UpsertDeployment {
  def apply[P <: Base](
      implicit
      executor: ExecutionContext,
      client: K8SRequestContext,
      lc: LoggingContext
    ): Processor[P] =
    (p: SResource[P]) => {
      result(
        Try(
          SimpleDeployer(client)
            .createOrUpdate(p.product)(executor, p.format, p.definition, lc)
        ).toEither
      )
    }

  def apply[P <: Base](
      p: SResource[P]
    )(implicit
      executor: ExecutionContext,
      client: K8SRequestContext,
      lc: LoggingContext
    ): MaybeResult[P] = apply[P].process(p)
}
