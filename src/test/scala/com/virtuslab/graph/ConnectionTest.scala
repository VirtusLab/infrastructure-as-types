package com.virtuslab.graph

import com.virtuslab.dsl.Namespace.NamespaceReference
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

    val frontendRoleLabel = RoleLabel("frontend")
    val frontendNsRef = Namespace("test")
      .labeled(frontendRoleLabel)

    val backendRoleLabel = RoleLabel("backend")
    val backendNsRef = Namespace("backend")
      .labeled(backendRoleLabel)

    val app3 = Application("app-two", "app-two-image")

    val backend = backendNsRef
      .inNamespace { implicit ns =>
        import ns._

        Applications(
          app3
        )

        app3 communicatesWith frontendNsRef

        ns
      }

    val app1 = Application("app-one", "image-app-one", labels = Set(frontendRoleLabel), ports = Networked.Port(9090) :: Nil)
    val app2 = Application("app-two", "image-app-two", labels = Set(frontendRoleLabel), ports = Networked.Port(9090) :: Nil)

    val frontend = frontendNsRef
      .inNamespace { implicit ns =>
        import ns._
//        val conn1 = Connection(
//          ApplicationSelector(Labels(backendRoleLabel)),
//          NamespaceSelector(Labels(frontendRoleLabel))
//        )

        Applications(
          app1,
          app2
        )

        Connections(
          app1 communicatesWith app2,
          app1 communicatesWith backendNsRef
        )

        ns
      }
  }
}
