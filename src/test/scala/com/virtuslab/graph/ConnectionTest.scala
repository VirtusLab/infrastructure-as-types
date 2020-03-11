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

    class Connection[A: Selectable, B: Selectable, C: Selectable](
        namespace: Namespace,
        resourceSelector: Selector[A],
        ingress: Selector[B],
        egress: Selector[C])

    object Connection {
      def apply[A: Selectable, B: Selectable, C: Selectable](
          resourceSelector: Selector[A],
          ingress: Selector[B] = EmptySelector,
          egress: Selector[C] = EmptySelector
        )(implicit
          ns: Namespace
        ): Connection[A, B, C] =
        new Connection(ns, resourceSelector, ingress, egress)
    }

    case class Applications(defined: Set[Application], namespace: Namespace)
    object Applications {
      def apply(defined: Set[Application])(implicit ns: Namespace): Applications = {
        Applications(defined, ns)
      }
    }

    case class Connections(defined: Set[Connection[_, _, _]])
    object Connections {

      //TODO: extract to common place for implicits
      implicit class ApplicationConnectionOps(app: Application) {
        def communicatesWith(other: Application)(implicit ns: Namespace): Connection[_, _, _] = {
          Connection(
            resourceSelector = ApplicationSelector(app),
            ingress = ApplicationSelector(other),
            egress = ApplicationSelector(app)
          )
        }

        def communicatesWith(namespace: NamespaceReference)(implicit ns: Namespace): Connection[_, _, _] = {
          Connection(
            resourceSelector = ApplicationSelector(app),
            ingress = NamespaceSelector(namespace),
            egress = ApplicationSelector(app)
          )
        }
      }
    }

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

        Applications {
          Set(app3)
        }(ns)

        Connections {
          import Connections._

          Set(
            app3 communicatesWith frontendNsRef
          )
        }(ns)

        ns
      }

    val app1 = Application("app-one", "image-app-one", labels = Set(frontendRoleLabel), ports = Networked.Port(9090) :: Nil)
    val app2 = Application("app-two", "image-app-two", labels = Set(frontendRoleLabel), ports = Networked.Port(9090) :: Nil)

    val frontend = frontendNsRef
      .inNamespace { implicit ns =>
//        val conn1 = Connection(
//          ApplicationSelector(Labels(backendRoleLabel)),
//          NamespaceSelector(Labels(frontendRoleLabel))
//        )

        Applications {
          Set(app1, app2)
        }

        Connections {
          import Connections._

          Set(
            app1 communicatesWith app2,
            app1 communicatesWith backendNsRef
          )
        }

        ???
      }
  }
}
