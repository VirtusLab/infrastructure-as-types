package com.virtuslab.iat.materialization.skuber

import com.virtuslab.iat.kubernetes.skuber.{ Base, STransformer }
import play.api.libs.json.Format
import skuber.api.client.LoggingContext
import skuber.{ K8SRequestContext, ResourceDefinition }

import scala.concurrent.ExecutionContext

trait UpsertDeployment {
  implicit def transformer[P <: Base](
      implicit
      executor: ExecutionContext,
      client: K8SRequestContext,
      lc: LoggingContext
    ): STransformer[P, Base] = new STransformer[P, Base] {
    override def apply(p: P)(implicit f: Format[P], d: ResourceDefinition[P]): Base =
      SimpleDeployer(client).createOrUpdate(p)
  }
}
