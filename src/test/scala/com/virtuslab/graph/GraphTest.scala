package com.virtuslab.graph

import com.virtuslab.dsl.{Namespace, Resource}
import com.virtuslab.internal.Pod
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class GraphTest extends AnyFlatSpec with Matchers {
  it should "allow to express a connection between two applications" in {
    // TODO validation
    type LabelKey = String // "[prefix/]name" where name is required (same format as value) and prefix is optional (DNS Subdomain Name format)
    type LabelValue = String // 63 characters, [a-z0-9A-Z] with (-_.), basically DNS Label Names with dots and underscores allowed

    trait Label {
      def name: LabelKey
      def value: LabelValue
    }

    trait Labels {
      def labels: Set[Label]
    }
    object Labels {
      def apply(values: Label*): Labels = apply(values.toSet)
      def apply(values: Set[Label]): Labels = new Labels {
        override def labels: Set[Label] = values
      }
    }

    final case class NoLabels() extends Labels {
      override def labels: Set[Label] = Set()
    }

    case class NameLabel(value: LabelValue) extends Label {
      override def name: LabelKey = "name"
    }

    case class RoleLabel(value: LabelValue) extends Label {
      override def name: LabelKey = "role"
    }

    class BackendNamespace extends Namespace with Labels {
      override def name: String = "backend"

      override def labels: Set[Label] = Set(
        NameLabel("backend"),
        RoleLabel("backend")
      )
    }

    class FrontendNamespace extends Namespace with Labels {
      override def name: String = "frontend"

      override def labels: Set[Label] = Set(
        NameLabel("frontend"),
        RoleLabel("frontend")
      )
    }

    // a simple selector "matchLabels" that compares two maps
    //    but there is also "matchExpressions" that has []LabelSelectorRequirement
    type IPBlock = String // TODO a proper object, a CIDR
    type LabelRequirement = String // TODO a proper expression DLS with =, ==, and != and sets
    type LabelRequirements = Set[LabelRequirement]

    trait Selectable[T] {
      def selected: T
      def matches(target: T): Boolean
    }

    case class LabelWitness(selected: Labels) extends Selectable[Labels] {
      override def matches(target: Labels): Boolean = target == selected
    }
    case class LabelRequirementWitness(selected: LabelRequirements) extends Selectable[LabelRequirements] {
      override def matches(target: LabelRequirements): Boolean = ???
    }
    case class IPBlockWitness(selected: IPBlock) extends Selectable[IPBlock] {
      override def matches(target: IPBlock): Boolean = ???
    } // specific to NetworkPolicyPeer

    // TODO there should be PodSelector and NamespaceSelector
    class Selector[A, +R <: Resource with Labels, S <: Selectable[A]](selectable: S) {
      def selects(resource: R): Boolean = selectable.matches(resource.labels)
    }

    // TODO check NodeSelector

    object NamespaceSelector {
      def apply[S : Selectable](selectable: S): Selector[Namespace, S] = new Selector[Namespace, S](selectable)
    }

    object PodSelector {
      def apply[S : Selectable](selectable: S): Selector[Pod, S] = new Selector[Pod, S](selectable)
    }

    object EmptySelector {
      def apply(): Selector[Resource, Labels] = new Selector[Resource, Labels](NoLabels())
    }

    case class Connection[+R <: Resource](
             resourceSelector: Selector[R, Labels],
             ingress: Selector[R, Labels],
             egress: Selector[R, Labels])

    val frontendToBackend = Connection(
      resourceSelector = PodSelector(
        Labels(
          NameLabel("frontend")
        )
      ),
      ingress = EmptySelector(),
      egress = PodSelector(
        Labels(
          NameLabel("backend")
        )
      )
    )

    val backendFromFrontend = Connection(
      resourceSelector = PodSelector(
        Labels(
          NameLabel("backend")
        )
      ),
      ingress = PodSelector(
        Labels(
          NameLabel("frontend")
        )
      ),
      egress = EmptySelector()
    )


  }
}
