package com.virtuslab.iat.openapitest

import com.virtuslab.iat
import com.virtuslab.iat.kubernetes.meta.Metadata
import com.virtuslab.kubernetes.client.openapi.model
import com.virtuslab.kubernetes.client.openapi.model.ObjectMeta
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class MetadataExtractorTest extends AnyFlatSpec with Matchers {
  it should "extract metadata from JValue" in {
    val ns = model.Namespace(
      apiVersion = Some("v1"),
      kind = Some("Namespace"),
      metadata = Some(
        ObjectMeta(
          name = Some("ns1")
        )
      )
    )
    val dp = model.Deployment(
      apiVersion = Some("apps/v1"),
      kind = Some("Deployment"),
      metadata = Some(
        ObjectMeta(
          name = Some("app1"),
          namespace = Some("ns1")
        )
      )
    )

    import iat.openapi.json4s._

    val nsJson = asJValue(ns)
    val nsMeta = asMeta(nsJson)
    nsMeta should equal(Right(Metadata("v1", "Namespace", "", "ns1")))

    val dpJson = asJValue(dp)
    val dpMeta = asMeta(dpJson)
    dpMeta should equal(Right(Metadata("apps/v1", "Deployment", "ns1", "app1")))
  }
}
