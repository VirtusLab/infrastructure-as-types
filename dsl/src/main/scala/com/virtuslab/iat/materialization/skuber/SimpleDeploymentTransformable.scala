package com.virtuslab.iat.materialization.skuber

import com.virtuslab.iat.core.Transformable
import com.virtuslab.iat.core.Transformable.Transformer
import play.api.libs.json.Format
import skuber.api.client.LoggingContext
import skuber.{ K8SRequestContext, ResourceDefinition }

import scala.concurrent.ExecutionContext

trait SimpleDeploymentTransformable {
  implicit def transformer[P <: skuber.ObjectResource](
      implicit
      executor: ExecutionContext,
      client: K8SRequestContext,
      format: Format[P],
      definition: ResourceDefinition[P],
      lc: LoggingContext
    ): Transformer[P, P] =
    p =>
      new Transformable[P, P] {
        override def transform: P = SimpleDeployer(client).createOrUpdate(p)
      }
}
