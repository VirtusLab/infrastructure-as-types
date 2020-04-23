package com.virtuslab.iat.kubernetes.openapitest

import com.virtuslab.iat.core.Transformable.Transformer
import com.virtuslab.iat.kubernetes.{ openapi, Metadata }
import com.virtuslab.kubernetes.client.openapi.model
import com.virtuslab.kubernetes.client.openapi.model.ObjectMeta
import org.json4s.JValue
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class MetadataExtractorTest extends AnyFlatSpec with Matchers {
  it should "extract metadata from JValue" in {
    import openapi.json4s._

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

    val metaTransformer = implicitly[Transformer[JValue, Either[String, Metadata]]]

    val nsJson = asJValue(ns)
    val nsMeta = metaTransformer(nsJson).transform
    nsMeta should equal(Right(Metadata("v1", "Namespace", "", "ns1")))

    val dpJson = asJValue(dp)
    val dpMeta = metaTransformer(dpJson).transform
    dpMeta should equal(Right(Metadata("apps/v1", "Deployment", "ns1", "app1")))
  }
}
