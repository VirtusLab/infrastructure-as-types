package com.virtuslab.graph

import com.virtuslab.dsl._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ConnectionTest extends AnyFlatSpec with Matchers {
  it should "allow to express a connection between two applications" in {

//    type IP = String // TODO a proper object
//    type IPBlock = String // TODO a proper object, a CIDR
//    def matches(s: IPBlock): IP => Boolean = ??? // specific to NetworkPolicyPeer

    case class RoleLabel(value: Label#Value) extends Label {
      override def name: Key = "role"
    }

    val system = System("test")
    implicit val systemBuilder: SystemBuilder = system.builder

    val frontendRoleLabel = RoleLabel("frontend")
    val frontendNsRef = Namespace.ref("test", frontendRoleLabel)

    val backendRoleLabel = RoleLabel("backend")
    val backendNsRef = Namespace.ref("backend", backendRoleLabel)

    val app3 = Application.ref("app-two", "app-two-image")

    val backend = backendNsRef
      .inNamespace { implicit ns =>
        import ns._

        applications(
          app3
        )

        connections(
          app3 communicatesWith frontendNsRef
        )
        ns
      }

    val app1 = Application.ref("app-one", "image-app-one", labels = Set(frontendRoleLabel), ports = Networked.Port(9090) :: Nil)
    val app2 = Application.ref("app-two", "image-app-two", labels = Set(frontendRoleLabel), ports = Networked.Port(9090) :: Nil)

    val frontend = frontendNsRef
      .inNamespace { implicit ns =>
        import ns._

        applications(
          app1,
          app2
        )

        connections(
          app1 communicatesWith app2,
          app1 communicatesWith backendNsRef
//        app1 communicatesWith application(partition in (customerA, customerB), environment!=qa)
//        app1 communicatesWith namespace(partition in (customerA, customerB), environment!=qa)
        )
      }
  }
}
