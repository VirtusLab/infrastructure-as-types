package com.virtuslab.graph

import cats.data.NonEmptyList
import com.virtuslab.dsl.Application.DefinedApplication
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

    case class Connections(defined: Set[Connection[_, _, _]])
    object Connections {

      //TODO: extract to common place for implicits
      implicit class ApplicationConnectionOps(app: DefinedApplication) {
        def communicatesWith(other: DefinedApplication): Connection[_, _, _] = {
          Connection(
            resourceSelector = ApplicationSelector(app),
            ingress = ApplicationSelector(other),
            egress = ApplicationSelector(app)
          )(implicitly, implicitly, implicitly, app.namespace)
        }

        def communicatesWith(ns: NamespaceReference): Connection[_, _, _] = {
          Connection(
            resourceSelector = ApplicationSelector(app),
            ingress = NamespaceSelector(ns),
            egress = ApplicationSelector(app)
          )(implicitly, implicitly, implicitly, app.namespace)
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
        val completedApp3 = app3.bind()

        Connections {
          import Connections._

          Set(
            completedApp3 communicatesWith frontendNsRef
          )
        }

        NonEmptyList.of(app3)
      }

    val app1 = Application("app-one", "image-app-one", labels = Set(frontendRoleLabel), ports = Networked.Port(9090) :: Nil)
    val app2 = Application("app-two", "image-app-two", labels = Set(frontendRoleLabel), ports = Networked.Port(9090) :: Nil)

    val frontend = frontendNsRef
      .inNamespace { implicit ns =>

        val bindedApp1 = app1.bind()
        val bindedApp2 = app2.bind()

//        val conn1 = Connection(
//          ApplicationSelector(Labels(backendRoleLabel)),
//          NamespaceSelector(Labels(frontendRoleLabel))
//        )

        Connections {
          import Connections._

          Set(
            bindedApp1 communicatesWith bindedApp2,
            bindedApp1 communicatesWith backendNsRef
          )
        }

        NonEmptyList.of(app1, app2)
      }
  }
}
