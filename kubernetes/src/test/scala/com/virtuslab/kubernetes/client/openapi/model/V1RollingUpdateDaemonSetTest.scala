package com.virtuslab.kubernetes.client.openapi.model

import com.google.gson.Gson
import com.virtuslab.kubernetes.client.custom.{IntOrString, JSON}
import org.specs2.matcher.JsonMatchers
import org.specs2.mutable.Specification

class V1RollingUpdateDaemonSetTest extends Specification with JsonMatchers {
  val gson: Gson = JSON.gson

  "A V1RollingUpdateDaemonSet can be marshalled to JSON\n" >> {
    "where a value of '10' is used" >> {
      val json = gson.toJson(V1RollingUpdateDaemonSet(Some(IntOrString(10))))
      json must beEqualTo("""{"maxUnavailable":10}""")
    }
  }

  "A V1RollingUpdateDaemonSet can be un-marshalled from JSON\n" >> {
    "where a value of '10' is used" >> {
      val json = """{"maxUnavailable":10}"""
      gson.fromJson(json, classOf[V1RollingUpdateDaemonSet]) mustEqual V1RollingUpdateDaemonSet(Some(IntOrString(10)))
    }
  }
}
