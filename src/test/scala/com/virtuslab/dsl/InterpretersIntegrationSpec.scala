package com.virtuslab.dsl

import com.stephenn.scalatest.playjson.JsonMatchers
import com.virtuslab.dsl.interpreter.SystemInterpreter
import com.virtuslab.internal.{ ShortMeta, SkuberConverter }
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class InterpretersIntegrationSpec extends AnyFlatSpec with Matchers with JsonMatchers {

  it should "create a simple system" in {
    val system = DistributedSystem.ref(this.getClass.getCanonicalName).inSystem { implicit ds =>
      import ds._

      namespaces(
        Namespace.ref("test").inNamespace { implicit ns =>
          import ns._

          applications(
            Application(Labels(Name("app-one")), "image-app-one", ports = Networked.Port(9090) :: Nil),
            Application(Labels(Name("app-two")), "image-app-two", ports = Networked.Port(9090, Some("http-port")) :: Nil)
          )
        }
      )
    }

    val systemInterpreter = SystemInterpreter.of(system)

    SkuberConverter(systemInterpreter).toMetaAndJsValue foreach {
      case (ShortMeta(_, "Service", _, "app-one"), json) => json should matchJsonString("""
{
  "apiVersion":"v1",
  "kind":"Service",
  "metadata":{
    "name":"app-one",
    "namespace":"test",
    "labels":{
      "name":"app-one"
    }
  },
  "spec":{
    "type":"ClusterIP",
    "selector":{
      "name":"app-one"
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
    "namespace":"test",
    "labels":{
      "name":"app-one"
    }
  },
  "spec":{
    "selector":{
      "matchLabels":{
        "name":"app-one"
      }
    },
    "replicas":1,
    "template":{
      "metadata":{
        "labels":{
          "name":"app-one"
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
      "name":"app-two"
    }
  },
  "spec":{
    "type":"ClusterIP",
    "sessionAffinity":"None",
    "ports":[
      {"name":"http-port","protocol":"TCP","port":9090,"targetPort":9090}
    ],
    "selector":{
      "name":"app-two"
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
    "namespace":"test",
    "labels":{
      "name":"app-two"
    }
  },
  "spec":{
    "selector":{
      "matchLabels":{
        "name":"app-two"
      }
    },
    "replicas":1,
    "template":{
      "metadata":{
        "labels":{
          "name":"app-two"
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
      case (ShortMeta(_, "Namespace", _, "test"), json)     => json should matchJsonString("""
{
  "kind":"Namespace",
  "apiVersion":"v1",
  "metadata": {
    "name":"test",
    "labels":{
      "name":"test"
    }
  }
}
""")
      case (m, _)                                           => throw new IllegalArgumentException(s"Resource $m was not matched")
    }
  }
}
