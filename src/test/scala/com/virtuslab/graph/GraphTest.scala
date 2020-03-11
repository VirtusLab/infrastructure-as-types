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

    class Connection[A: Selectable, B: Selectable, C: Selectable](
        namespace: Namespace,
        resourceSelector: Selector[A],
        ingress: Selector[B],
        egress: Selector[C])

    object Connection {
      def apply[A: Selectable, B: Selectable, C: Selectable](
          resourceSelector: Selector[A],
          ingress: Selector[B] = EmptySelector(),
          egress: Selector[C] = EmptySelector()
        )(implicit
          ns: Namespace
        ): Connection[A, B, C] =
        new Connection(ns, resourceSelector, ingress, egress)
    }

    case class RoleLabel(value: Label#Value) extends Label {
      override def name: Key = "role"
    }

    val backendRoleLabel = RoleLabel("backend")

    val backend = Namespace("backend")
      .labeled(backendRoleLabel)

    val frontendRoleLabel = RoleLabel("frontend")

    val frontend = Namespace("test")
      .labeled(frontendRoleLabel)
      .inNamespace { implicit ns =>
        {
          val app1 = Application("app-one", "image-app-one")
            .labeled(frontendRoleLabel)
            .listensOn(9090)
          val app2 = Application("app-two", "image-app-two")
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
