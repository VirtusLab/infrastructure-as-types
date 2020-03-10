package com.virtuslab.graph

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
             ingress: Selector[B],
             egress: Selector[C])

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

    val backendNamespace = new Namespaced with Labeled {
      override def namespace: Namespace = Namespace("backend")

      override def labels: Set[Label] = Set(
        NameLabel("backend"),
        RoleLabel("backend")
      )
    }

    val frontendNamespace = new Namespaced with Labeled {
      override def namespace: Namespace = Namespace("frontend")

      override def labels: Set[Label] = Set(
        NameLabel("frontend"),
        RoleLabel("frontend")
      )
    }

//    val app = HttpApplication("app-two", "image-app-two")
//      .listensOn(9090, "http-port")
//
//    frontendToBackend.resourceSelector.matches(app)
  }
}
