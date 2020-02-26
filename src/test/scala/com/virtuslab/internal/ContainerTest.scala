package com.virtuslab.internal

import cats.data.NonEmptyList
import com.stephenn.scalatest.playjson.JsonMatchers
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.Json

class ContainerTest extends AnyFlatSpec with Matchers with JsonMatchers {
  import skuber.json.format._

  it should "work for a simple case" in {
    val pod = Pod(
      ObjectMeta("app"),
      PodSpec(
        NonEmptyList.of(
          Container(
            "app",
            Image.parse("quay.io/virtuslab/cloud-file-server:v0.0.6")
          )
        )
      )
    )
    val json = Json.toJson(pod.toSkuber)

    json should matchJsonString("""
{
  "kind": "Pod",
  "apiVersion":"v1",
  "metadata":{
    "name": "app"
  },
  "spec": {
    "containers": [
      {
        "name": "app",
        "image": "quay.io/virtuslab/cloud-file-server:v0.0.6",
        "imagePullPolicy": "IfNotPresent"
      }
    ],
    "restartPolicy":"Always",
    "dnsPolicy":"ClusterFirst"
  }
}
""")
  }

}
