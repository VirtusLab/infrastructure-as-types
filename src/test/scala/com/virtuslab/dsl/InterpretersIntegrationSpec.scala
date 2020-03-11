package com.virtuslab.dsl

import cats.data.NonEmptyList
import com.stephenn.scalatest.playjson.JsonMatchers
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import com.virtuslab.internal.{ ShortMeta, SkuberConverter }

class InterpretersIntegrationSpec extends AnyFlatSpec with Matchers with JsonMatchers {

  it should "create a system" in {
    val ns = Namespace("test").inNamespace { implicit ns =>
      NonEmptyList.of(
        Application("app-one", "image-app-one")
          .listensOn(9090),
        Application("app-two", "image-app-two")
          .listensOn(9090, "http-port")
      )
    }

    val system = System("test-system")
      .inSystem(ns)

    val systemInterpreter = SystemInterpreter.of(system)

    SkuberConverter(systemInterpreter).toMetaAndJson(system) foreach {
      case (ShortMeta(_, "Service", _, "app-one"), json) => json should matchJsonString("""
{
  "apiVersion":"v1",
  "kind":"Service",
  "metadata":{
    "name":"app-one",
    "namespace":"test",
    "labels":{
      "system":"test-system",
      "app":"app-one"
    }
  },
  "spec":{
    "type":"ClusterIP",
    "selector":{
      "system":"test-system",
      "app":"app-one"
    },
    "ports":[
      {"protocol":"TCP","port":9090,"targetPort":9090}
    ],
    "sessionAffinity":"None"
  }
}
""")

      case (ShortMeta(_, "Deployment", _, "app-one"), json) => json should matchJsonString("""
{
  "apiVersion":"apps/v1",
  "kind":"Deployment",
  "metadata":{
    "name": "app-one",
    "namespace":"test"
  },
  "spec":{
    "selector":{
      "matchLabels":{
        "system":"test-system",
        "app":"app-one"
      }
    },
    "replicas":1,
    "template":{
      "metadata":{
        "labels":{
          "system":"test-system",
          "app":"app-one"
        }
      },
      "spec":{
        "containers":[
          {
            "name":"app-one",
            "image":"image-app-one",
            "imagePullPolicy":"IfNotPresent",
            "ports":[
              {"containerPort":9090,"protocol":"TCP"}
            ]
          }
        ],
        "restartPolicy":"Always",
        "dnsPolicy":"ClusterFirst"
      }
    }
  }
}
""")

      case (ShortMeta(_, "Service", _, "app-two"), json) => json should matchJsonString("""
{
  "apiVersion":"v1",
  "kind":"Service",
  "metadata":{
    "name":"app-two",
    "namespace":"test",
    "labels":{
      "system":"test-system",
      "app":"app-two"
    }
  },
  "spec":{
    "type":"ClusterIP",
    "sessionAffinity":"None",
    "ports":[
      {"name":"http-port","protocol":"TCP","port":9090,"targetPort":9090}
    ],
    "selector":{
      "system":"test-system",
      "app":"app-two"
    }
  }
}
""")

      case (ShortMeta(_, "Deployment", _, "app-two"), json) => json should matchJsonString("""
{
  "kind":"Deployment",
  "apiVersion":"apps/v1",
  "metadata":{
    "name": "app-two",
    "namespace":"test"
  },
  "spec":{
    "selector":{
      "matchLabels":{
        "system":"test-system",
        "app":"app-two"
      }
    },
    "replicas":1,
    "template":{
      "metadata":{
        "labels":{
          "system":"test-system",
          "app":"app-two"
        }
      },
      "spec":{
        "containers":[
          {
            "name":"app-two",
            "image":"image-app-two",
            "imagePullPolicy":"IfNotPresent",
            "ports":[
              {"containerPort":9090,"protocol":"TCP","name":"http-port"}
            ]
          }
        ],
        "restartPolicy":"Always",
        "dnsPolicy":"ClusterFirst"
      }
    }
  }
}
""")
      case (ShortMeta(_, "Namespace", _, _), json)          => json should matchJsonString("""
{
  "kind":"Namespace",
  "apiVersion":"v1",
  "metadata": {
    "name":"test"
  }
}
""")
      case (m, _)                                           => throw new IllegalArgumentException(s"Resource $m was not matched")
    }
  }
}
