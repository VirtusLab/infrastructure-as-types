package com.virtuslab.iast

trait Implicits { self: GraphSpecific with ModelSpecific with TargetSpecific =>
  object implicits {
    implicit class HolderOps[H <: Holder[H, T], T <: Reference[H, T]](holder: Holder[H, T]) {
      def toIterable: Iterable[T] = holder.members
    }
  }
}

trait GraphSpecific {

  /**
    * Special case. A holder-less reference.
    * @tparam T is the object reference (this) that has no holder
    */
  trait DetachedReference[T <: DetachedReference[T]] { self: T =>
    def obj: T = self
  }

  /**
    * Reference wraps an object and has a reference to it's holder.
    * This reference will be automatically added to the holder members.
    *
    * Example: Namespace (H) <--is held-- Application (T)
    *
    * @tparam H is the holder that holds this T reference
    * @tparam T is the object reference (this) that is being held by the H holder
    */
  trait Reference[H <: Holder[H, T], T <: Reference[H, T]] extends DetachedReference[T] { self: T =>
    def holder: H
    holder._members += this
  }

  /**
    * Holder holds object references as its members.
    *
    * Example: Application (T) --holds--> Configuration(s) (R)
    * @tparam H is the holder, that holds the T references
    * @tparam T are the references that are being held by the H holder
    */
  trait Holder[H <: Holder[H, T], T <: Reference[H, T]] {

    /**
      * Creates a copy of the current members.
      * @return an immutable list from the current internal state
      */
    def members: List[T] = _members.toList

    import scala.collection.mutable
    private[GraphSpecific] val _members: mutable.Set[T] = mutable.Set[T]()
  }

  /**
    * Definition is a Holder and a Reference, combined.
    * It wraps an object reference, has a reference it its holder and holds its own members.
    *
    * Example: Namespace (H) <--is held-- Application (T) --holds--> Configuration(s) (R)
    *
    * @tparam H is the holder that holds this T definition
    * @tparam T is the object reference (this) that is being held by the H holder
    * @tparam R are the object references that are being held by this T definition
    */
  trait Definition[H <: Holder[H, T], T <: Definition[H, T, R], R <: Reference[T, R]] extends Reference[H, T] with Holder[T, R] {
    self: T =>
  }

  /**
    * Special case. A holder-less definition.
    *
    * Example: Nothing <--is held-- DistributedSystem (T) --holds--> Namespace(s) (R)
    *
    * @tparam T is the object reference (this) that has no holder
    * @tparam R are the object references that are being held by this T definition
    */
  trait DetachedDefinition[T <: DetachedDefinition[T, R], R <: Reference[T, R]] extends DetachedReference[T] with Holder[T, R] {
    self: T =>
  }
}

trait ModelSpecific { self: GraphSpecific =>
  type Meta
  type Base
  type JsonAST

  trait Resource[B <: Base] {
    def meta: Meta
    def obj: B
    def asJsonAST: JsonAST
  }

  trait DefinitionInterpreter1[H <: Holder[H, T], T <: Definition[H, T, R], R <: Reference[T, R], B <: Base]
    extends (T => Resource[B])
  trait ReferenceInterpreter1[H <: Holder[H, T], T <: Reference[H, T], B <: Base] extends (T => Resource[B])
  trait HolderInterpreter1[T <: DetachedDefinition[T, R], R <: Reference[T, R], B <: Base] extends (T => Resource[B])

  trait DefinitionInterpreter2[H <: Holder[H, T], T <: Definition[H, T, R], R <: Reference[T, R], B1 <: Base, B2 <: Base]
    extends (T => (Resource[B1], Resource[B2]))
  trait ReferenceInterpreter2[H <: Holder[H, T], T <: Reference[H, T], B1 <: Base, B2 <: Base]
    extends (T => (Resource[B1], Resource[B2]))
  trait HolderInterpreter2[T <: DetachedDefinition[T, R], R <: Reference[T, R], B1 <: Base, B2 <: Base]
    extends (T => (Resource[B1], Resource[B2]))

  // TODO Tuple3..22
}

trait TargetSpecific { self: ModelSpecific =>
  trait Materializer[B <: Base, R] extends (Resource[B] => R)
  case class JValueMaterializer[B <: Base]() extends Materializer[B, JsonAST] {
    override def apply(r: Resource[B]): JsonAST = r.asJsonAST
  }
  case class MetaJValueMaterializer[B <: Base]() extends Materializer[B, (Meta, JsonAST)] {
    override def apply(r: Resource[B]): (Meta, JsonAST) = r.meta -> r.asJsonAST
  }
}

trait DistributedGraph extends GraphSpecific {
  import com.virtuslab.dsl.{ Labeled, Labels }

  trait DistributedSystem extends Labeled with DetachedReference[DistributedSystem] {
    def namespaces: List[Namespace]
    val _namespaces: DistributedSystem.Namespaces = DistributedSystem.Namespaces(this)
  }
  object DistributedSystem {
    case class Namespaces(system: DistributedSystem) extends Holder[Namespaces, Namespace]

    case class ADistributedSystem(labels: Labels) extends Labeled with DistributedSystem {
      override def namespaces: List[Namespace] = _namespaces.members
    }
    def apply(labels: Labels): DistributedSystem = ADistributedSystem(labels)
  }

  trait Namespace extends Labeled with Reference[DistributedSystem.Namespaces, Namespace] {
    val applications: Namespace.Applications = Namespace.Applications(this)
    val gateways: Namespace.Gateways = Namespace.Gateways(this)
  }
  object Namespace {
    case class Applications(namespace: Namespace) extends Holder[Applications, Application]
    case class Gateways(namespace: Namespace) extends Holder[Gateways, Gateway]

    case class ANamespace(labels: Labels, holder: DistributedSystem.Namespaces) extends Labeled with Namespace
    def apply(labels: Labels, system: DistributedSystem): Namespace = ANamespace(labels, system._namespaces)
  }

  trait Application extends Labeled with Reference[Namespace.Applications, Application] {
    val configurations: Application.Configurations = Application.Configurations(this)
  }

  object Application {
    case class Configurations(application: Application) extends Holder[Configurations, Configuration]

    case class AnApplication(labels: Labels, holder: Namespace.Applications) extends Labeled with Application
    def apply(labels: Labels, namespace: Namespace): Application = AnApplication(labels, namespace.applications)
  }

  trait Gateway extends Labeled with Reference[Namespace.Gateways, Gateway]
  object Gateway {
    case class AGateway(labels: Labels, holder: Namespace.Gateways) extends Labeled with Gateway
    def apply(labels: Labels, namespace: Namespace): Gateway = AGateway(labels, namespace.gateways)
  }
//  trait Connection extends Labeled with Definition[Namespace, Connection, Rule]

  trait Configuration extends Labeled with Reference[Application.Configurations, Configuration]
  object Configuration {
    case class AConfiguration(labels: Labels, holder: Application.Configurations) extends Labeled with Configuration
    def apply(labels: Labels, application: Application): Configuration = AConfiguration(labels, application.configurations)
  }
//  trait Rule extends Labeled with Reference[Connection, Rule]
}

trait KubernetesModel extends ModelSpecific { self: DistributedGraph =>
  import com.virtuslab.json.json4s.jackson.JsonMethods
  import com.virtuslab.kubernetes.client.openapi.core.ApiModel
  import com.virtuslab.kubernetes.client.openapi.model
  import com.virtuslab.kubernetes.client.openapi.model.{ Deployment, Service }
  import com.virtuslab.materializer.openapi.Metadata
  import org.json4s.JsonAST.JValue

  override type Meta = Metadata
  override type Base = ApiModel
  override type JsonAST = JValue

  case class KubernetesResource[B <: Base](metadata: Metadata, obj: B) extends Resource[B] {
    override def meta: Metadata = metadata
    override def asJsonAST: JValue = JsonMethods.asJValue(obj)
  }

  implicit case object NamespaceInterpreter
    extends ReferenceInterpreter1[DistributedSystem.Namespaces, Namespace, model.Namespace] {
    override def apply(namespace: Namespace): KubernetesResource[model.Namespace] = {
      val meta = Metadata("v1", "Namespace", "", namespace.obj.name)
      val ns = model.Namespace(
        apiVersion = Some(meta.apiVersion),
        kind = Some(meta.kind),
        metadata = Some(
          model.ObjectMeta(
            name = Some(namespace.obj.name),
            labels = Some(namespace.obj.labels.toMap)
          )
        )
      )
      KubernetesResource[model.Namespace](meta, ns)
    }
  }

  implicit case object ApplicationInterpreter
    extends ReferenceInterpreter2[Namespace.Applications, Application, model.Deployment, model.Service] {
    override def apply(app: Application): (Resource[Deployment], Resource[Service]) = {
      val metaDeploy = Metadata("apps/v1", "Deployment", app.holder.namespace.name, app.name)
      val deploy = model.Deployment(
        apiVersion = Some(metaDeploy.apiVersion),
        kind = Some(metaDeploy.kind),
        metadata = Some(
          model.ObjectMeta(
            name = Some(app.name),
            namespace = Some(app.holder.namespace.name),
            labels = Some(app.labels.toMap)
          )
        )
      )
      val metaSvc = Metadata("v1", "Service", app.holder.namespace.name, app.name)
      val svc = model.Service(
        apiVersion = Some(metaSvc.apiVersion),
        kind = Some(metaSvc.kind),
        metadata = Some(
          model.ObjectMeta(
            name = Some(app.name),
            namespace = Some(app.holder.namespace.name),
            labels = Some(app.labels.toMap)
          )
        )
      )

      (
        KubernetesResource[model.Deployment](metaDeploy, deploy),
        KubernetesResource[model.Service](metaSvc, svc)
      )
    }
  }
}

trait JValueTarget extends TargetSpecific { self: KubernetesModel =>
  import com.virtuslab.json.json4s.jackson.JsonMethods

  case class MetaStringMaterializer[B <: Base]() extends Materializer[B, (Meta, String)] {
    override def apply(r: Resource[B]): (Meta, String) = r.meta -> JsonMethods.pretty(r.asJsonAST)
  }
}
