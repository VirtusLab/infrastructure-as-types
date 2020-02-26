package com.virtuslab.dsl

import com.stephenn.scalatest.playjson.JsonMatchers
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.{ JsValue, Json }

class InterpretersIntegrationSpec extends AnyFlatSpec with Matchers with JsonMatchers {

  import skuber.json.format._

  case class Meta(
      apiVersion: String,
      kind: String,
      namespace: String,
      name: String)

  it should "create a system" in {
    val appOne = HttpApplication("app-one", "image-app-one")
      .listensOn(9090)

    val appTwo = HttpApplication("app-two", "image-app-two")
      .listensOn(9090, "http-port")

    val system = System("test-system")
      .addApplication(appOne)
      .addApplication(appTwo)

    val httpApplicationInterpreter = new HttpApplicationInterpreter(system).asInstanceOf[ApplicationInterpreter[Application]] // FIXME
    val systemInterpreter = new SystemInterpreter({
      case _: HttpApplication => httpApplicationInterpreter
    })

    val resources: Seq[(Meta, JsValue)] = systemInterpreter(system) flatMap {
      case (service, deployment) =>
        Seq(
          Meta(service.apiVersion, service.kind, service.ns, service.name) -> Json.toJson(service),
          Meta(deployment.apiVersion, deployment.kind, deployment.ns, deployment.name) -> Json.toJson(deployment)
        )
      case r => throw new IllegalArgumentException(s"Resource $r was not expected")
    }

    resources foreach {
      case (Meta(_, "Service", _, "app-one"), json) => json should matchJsonString("""
{
  "apiVersion":"v1",
  "kind":"Service",
  "metadata":{
    "name":"app-one",
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

      case (Meta(_, "Deployment", _, "app-one"), json) => json should matchJsonString("""
{
  "apiVersion":"apps/v1",
  "kind":"Deployment",
  "metadata":{
    "name": "app-one"
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

      case (Meta(_, "Service", _, "app-two"), json) => json should matchJsonString("""
{
  "apiVersion":"v1",
  "kind":"Service",
  "metadata":{
    "name":"app-two",
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

      case (Meta(_, "Deployment", _, "app-two"), json) => json should matchJsonString("""
{
  "kind":"Deployment",
  "apiVersion":"apps/v1",
  "metadata":{
    "name": "app-two"
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
      case (m, _)                                      => throw new IllegalArgumentException(s"Resource $m was not matched")
    }
  }
}
