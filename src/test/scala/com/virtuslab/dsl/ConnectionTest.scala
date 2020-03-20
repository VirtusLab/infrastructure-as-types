package com.virtuslab.dsl

import com.stephenn.scalatest.playjson.JsonMatchers
import com.virtuslab.dsl.interpreter.SystemInterpreter
import com.virtuslab.internal.{ ShortMeta, SkuberConverter }
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ConnectionTest extends AnyFlatSpec with Matchers with JsonMatchers {
  it should "allow to express a connection between two applications" in {

    //    type IP = String // TODO a proper object
    //    type IPBlock = String // TODO a proper object, a CIDR
    //    def matches(s: IPBlock): IP => Boolean = ??? // specific to NetworkPolicyPeer

    case class RoleLabel(value: String) extends Label {
      override val key: String = "role"
    }

    val system = DistributedSystem(this.getClass.getCanonicalName)
    implicit val systemBuilder: SystemBuilder = system.builder

    val frontendRoleLabel = RoleLabel("frontend")
    val frontendNsRef = Namespace.ref(Labels(Name("frontend"), frontendRoleLabel))

    val backendRoleLabel = RoleLabel("backend")
    val backendNsRef = Namespace.ref(Labels(Name("backend"), backendRoleLabel))

    val app3 = Application.ref(Labels(Name("app-three"), backendRoleLabel), "image-app-three")

    backendNsRef
      .inNamespace { implicit ns =>
        import ns._

        applications(
          app3
        )

        connections(
          app3 communicatesWith frontendNsRef
        )
      }

    val app1 = Application.ref(Labels(Name("app-one"), frontendRoleLabel), "image-app-one", ports = Networked.Port(9090) :: Nil)
    val app2 = Application.ref(Labels(Name("app-two"), frontendRoleLabel), "image-app-two", ports = Networked.Port(9090) :: Nil)

    frontendNsRef
      .inNamespace { implicit ns =>
        import ns._

        applications(
          app1,
          app2
        )

        connections(
          app1 communicatesWith app2,
          app1 communicatesWith backendNsRef
        )
      }

    val systemInterpreter = SystemInterpreter.of(systemBuilder)
    val resources = SkuberConverter(systemInterpreter).toMetaAndJson

    resources should have length 11
    resources foreach {
      case (ShortMeta(_, "Namespace", _, "frontend"), json)                             => json should matchJsonString("""
{
  "kind":"Namespace",
  "apiVersion":"v1",
  "metadata": {
    "name":"frontend",
    "labels": {
      "name":"frontend",
      "role":"frontend"
    }
  }
}
""")
      case (ShortMeta(_, "Namespace", _, "backend"), json)                              => json should matchJsonString("""
{
  "kind":"Namespace",
  "apiVersion":"v1",
  "metadata": {
    "name":"backend",
    "labels": {
      "name":"backend",
      "role":"backend"
    }
  }
}
""")
      case (ShortMeta(_, "Service", "frontend", "app-one"), json)                       => json should matchJsonString("""
{
  "kind":"Service",
  "apiVersion":"v1",
  "metadata": {
    "name":"app-one",
    "namespace":"frontend",
    "labels": {
      "name":"app-one",
      "role":"frontend"
    }
  },
  "spec": {
    "ports": [
      {"protocol":"TCP","port":9090,"targetPort":9090}
    ],
    "selector": {
      "name":"app-one",
      "role":"frontend"
    },
    "type":"ClusterIP",
    "sessionAffinity":"None"
  }
}
""")
      case (ShortMeta(_, "Service", "frontend", "app-two"), json)                       => json should matchJsonString("""
{
  "kind":"Service",
  "apiVersion":"v1",
  "metadata": {
    "name":"app-two",
    "namespace":"frontend",
    "labels": {
      "name":"app-two",
      "role":"frontend"
    }
  },
  "spec": {
    "ports": [
      {"protocol":"TCP","port":9090,"targetPort":9090}
    ],
    "selector": {
      "name":"app-two",
      "role":"frontend"
    },
    "type":"ClusterIP",
    "sessionAffinity":"None"
  }
}
""")
      case (ShortMeta(_, "Service", "backend", "app-three"), json)                      => json should matchJsonString("""
{
  "kind":"Service",
  "apiVersion":"v1",
  "metadata": {
    "name":"app-three",
    "namespace":"backend",
    "labels": {
      "name":"app-three",
      "role":"backend"
    }
  },
  "spec": {
    "selector": {
      "name":"app-three",
      "role":"backend"
    },
    "type":"ClusterIP",
    "sessionAffinity":"None"
  }
}
""")
      case (ShortMeta(_, "Deployment", "backend", "app-three"), json)                   => json should matchJsonString("""
{
  "kind":"Deployment",
  "apiVersion":"apps/v1",
  "metadata": {
    "name":"app-three",
    "namespace":"backend",
    "labels": {
      "name":"app-three",
      "role":"backend"
    }
  },
  "spec": {
    "replicas":1,
    "selector": {
      "matchLabels": {
        "name":"app-three",
        "role":"backend"
      }
    },
    "template": {
      "metadata": {
        "labels": {
          "name":"app-three",
          "role":"backend"
        }
      },
      "spec": {
        "containers": [
          {
            "name":"app-three",
            "image":"image-app-three",
            "imagePullPolicy":"IfNotPresent"
          }
        ],
        "restartPolicy":"Always",
        "dnsPolicy":"ClusterFirst"
      }
    }
  }
}
""")
      case (ShortMeta(_, "Deployment", "frontend", "app-two"), json)                    => json should matchJsonString("""
{
  "kind":"Deployment",
  "apiVersion":"apps/v1",
  "metadata": {
    "name":"app-two",
    "namespace":"frontend",
    "labels":{
      "name":"app-two",
      "role":"frontend"
    }
  },
  "spec": {
    "replicas":1,
    "selector": {
      "matchLabels":{
        "name":"app-two",
        "role":"frontend"
      }
    },
    "template":{
      "metadata":{
        "labels":{
          "name":"app-two",
          "role":"frontend"
        }
      },
      "spec":{
        "containers":[
          {
            "name":"app-two",
            "image":"image-app-two",
            "ports":[
              {"containerPort":9090,"protocol":"TCP"}
            ],
            "imagePullPolicy":"IfNotPresent"
          }
        ],
        "restartPolicy":"Always",
        "dnsPolicy":"ClusterFirst"
      }
    }
  }
}
""")
      case (ShortMeta(_, "Deployment", "frontend", "app-one"), json)                    => json should matchJsonString("""
{
  "kind":"Deployment",
  "apiVersion":"apps/v1",
  "metadata": {
    "name":"app-one",
    "namespace":"frontend",
    "labels":{
      "name":"app-one",
      "role":"frontend"
    }
  },
  "spec": {
    "replicas":1,
    "selector":{
      "matchLabels":{
        "name":"app-one",
        "role":"frontend"
      }
    },
    "template":{
      "metadata":{
        "labels":{
          "name":"app-one",
          "role":"frontend"
        }
      },
      "spec":{
        "containers":[
          {
            "name":"app-one",
            "image":"image-app-one",
            "ports":[
              {"containerPort":9090,"protocol":"TCP"}
            ],
            "imagePullPolicy":"IfNotPresent"
          }
        ],
        "restartPolicy":"Always",
        "dnsPolicy":"ClusterFirst"
      }
    }
  }
}
""")
      case (ShortMeta(_, "NetworkPolicy", "frontend", "app-one-backend-app-one"), json) => json should matchJsonString("""
{
  "kind":"NetworkPolicy",
  "apiVersion":"networking.k8s.io/v1",
  "metadata":{
    "name":"app-one-backend-app-one",
    "namespace":"frontend",
    "labels":{
      "name":"app-one-backend-app-one"
    }
  }
}
""")
      case (ShortMeta(_, "NetworkPolicy", "backend", "app-three-frontend-app-three"), json) =>
        json should matchJsonString("""
{
  "kind":"NetworkPolicy",
  "apiVersion":"networking.k8s.io/v1",
  "metadata":{
    "name":"app-three-frontend-app-three",
    "namespace":"backend",
    "labels":{
      "name":"app-three-frontend-app-three"
    }
  }
}
""")
      case (ShortMeta(_, "NetworkPolicy", "frontend", "app-one-app-two-app-one"), json) => json should matchJsonString("""
{
  "kind":"NetworkPolicy",
  "apiVersion":"networking.k8s.io/v1",
  "metadata":{
    "name":"app-one-app-two-app-one",
    "namespace":"frontend",
    "labels":{
      "name":"app-one-app-two-app-one"
    }
  }
}
""")
      case (m, _)                                                                       => throw new IllegalArgumentException(s"Resource $m was not matched")
    }
  }
}
