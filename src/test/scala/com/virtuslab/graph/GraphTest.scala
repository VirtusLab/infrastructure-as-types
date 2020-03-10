package com.virtuslab.graph

import cats.data.NonEmptyList
import com.virtuslab.dsl._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class GraphTest extends AnyFlatSpec with Matchers {
  it should "allow to express a connection between two applications" in {

//    type IP = String // TODO a proper object
//    type IPBlock = String // TODO a proper object, a CIDR
//    def matches(s: IPBlock): IP => Boolean = ??? // specific to NetworkPolicyPeer

    case class Connection[A : Selectable, B : Selectable, C: Selectable](
             resourceSelector: ApplicationSelector[A],
             ingress: Selector[B] = EmptySelector(),
             egress: Selector[C] = EmptySelector())

    val frontendToBackend = Connection(
      resourceSelector = ApplicationSelector(
        Labels(
          NameLabel("frontend")
        )
      ),
      ingress = EmptySelector(),
      egress = ApplicationSelector(
        Labels(
          NameLabel("backend")
        )
      )
    )

    val backendFromFrontend = Connection(
      resourceSelector = ApplicationSelector(
        Labels(
          NameLabel("backend")
        )
      ),
      ingress = NamespaceSelector(
        Labels(
          NameLabel("frontend")
        )
      ),
      egress = EmptySelector()
    )

    case class RoleLabel(value: Label#Value) extends Label {
      override def name: Key = "role"
    }

    val backendRoleLabel = RoleLabel("backend")

    val backend = Namespace("backend")
      .labeled(backendRoleLabel)

    val frontendRoleLabel = RoleLabel("frontend")

    val frontend = Namespace("test")
      .labeled(frontendRoleLabel)
      .inNamespace { implicit ns => {
        val app1 = HttpApplication("app-one", "image-app-one")
            .labeled(frontendRoleLabel)
            .listensOn(9090)
        val app2 = HttpApplication("app-two", "image-app-two")
            .labeled(frontendRoleLabel)
            .listensOn(9090, "http-port")

        val conn1 = Connection(
          ApplicationSelector(Labels(backendRoleLabel)),
          NamespaceSelector(Labels(frontendRoleLabel))
        )

        NonEmptyList.of(app1, app2)
      }
    }
  }
}
