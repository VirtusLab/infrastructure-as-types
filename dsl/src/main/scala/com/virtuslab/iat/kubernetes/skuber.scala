package com.virtuslab.iat.kubernetes

import _root_.skuber.apps.v1.Deployment
import _root_.skuber.{ ConfigMap, Container, EnvVar, HTTPGetAction, LabelSelector, ObjectMeta, ObjectResource, Pod, Probe, Service, Volume, Namespace => SNamespace, Protocol => SProtocol, Secret => SSecret }
import _root_.skuber.ext.Ingress
import _root_.skuber.networking.NetworkPolicy
import _root_.skuber.networking.NetworkPolicy.{ EgressRule, IPBlock, IngressRule, Peer, Port, Spec }
import _root_.skuber.ResourceDefinition
import com.virtuslab.iat.core
import com.virtuslab.iat.core.Support
import com.virtuslab.iat.dsl.Port.{ APort, NamedPort }
import com.virtuslab.iat.dsl._
import com.virtuslab.iat.dsl.kubernetes.{ Namespace, _ }
import com.virtuslab.iat.json.playjson.Yaml
import com.virtuslab.iat.materialization.skuber.{ ObjectResourceMetadataExtractor, PlayJsonTransformable, UpsertDeployment }
import play.api.libs.json.{ Format, JsValue, Json, Writes }

object skuber {

  type Base = ObjectResource
  type RootInterpreter[A, R] = core.RootInterpreter[A, Base, R]
  type Interpreter[A, C, R] = core.Interpreter[A, C, Base, R]

  trait STransformer[P <: Base, R] {
    def apply(p: P)(implicit f: Format[P], d: ResourceDefinition[P]): R
  }

  case class SSupport[P <: Base: Format: ResourceDefinition, R](product: P, transformer: STransformer[P, R])
    extends Support[P, R] {
    def format: Format[P] = implicitly[Format[P]]
    def definition: ResourceDefinition[P] = implicitly[ResourceDefinition[P]]
    override def result: R = {
      transformer(product)(format, definition)
    }
  }

  object SSupport {
    def apply[P <: Base: Format: ResourceDefinition, R](p: P)(implicit t: STransformer[P, R]): Support[P, R] =
      SSupport[P, R](p, t)
  }

  object deployment {
    object InterpreterDerivation extends core.InterpreterDerivation[Namespace, Base, List[Base]]
    object Upsert extends UpsertDeployment
  }

  object metadata extends ObjectResourceMetadataExtractor {
    object InterpreterDerivation extends core.InterpreterDerivation[Namespace, Base, (Metadata, JsValue)]
  }

  object playjson extends PlayJsonTransformable {
    import play.api.libs.json.JsValue

    object InterpreterDerivation extends core.InterpreterDerivation[Namespace, Base, JsValue]

    def asJsValue[T: Writes](o: T): JsValue = Json.toJson(o)
    def asJsonString[T: Writes](o: T): String = Json.prettyPrint(asJsValue(o))
    def asYamlString[T: Writes](o: T): String = Yaml.prettyPrint(asJsValue(o))
  }

  import _root_.skuber.json.format._
  import _root_.skuber.json.ext.format._

  import Label.ops._

  def interpret[A, R](obj: A)(implicit i: RootInterpreter[A, R]): List[Support[_ <: Base, R]] =
    core.Interpreter.interpret(obj)
  def interpret[A, C, R](obj: A, ctx: C)(implicit i: Interpreter[A, C, R]): List[Support[_ <: Base, R]] =
    core.Interpreter.interpret(obj, ctx)

  implicit def namespaceInterpreter[R](
      implicit
      t: STransformer[SNamespace, R]
    ): RootInterpreter[Namespace, R] =
    (obj: Namespace) => {
      val namespace = SNamespace.from(
        ObjectMeta(
          name = obj.name,
          labels = obj.labels.asMap
        )
      )

      SSupport(namespace) :: Nil
    }

  implicit def configurationInterpreter[R](
      implicit
      t: STransformer[ConfigMap, R]
    ): Interpreter[Configuration, Namespace, R] =
    (obj: Configuration, ns: Namespace) => {
      val conf = ConfigMap(
        metadata = subinterpreter.objectMetaInterpreter(obj, ns),
        data = obj.data
      )

      SSupport(conf) :: Nil
    }

  implicit def secretInterpreter[R](
      implicit
      t: STransformer[SSecret, R]
    ): Interpreter[Secret, Namespace, R] =
    (obj: Secret, ns: Namespace) => {
      val secret = SSecret(
        metadata = subinterpreter.objectMetaInterpreter(obj, ns),
        data = obj.data.view.mapValues(_.getBytes).toMap
      )

      SSupport(secret) :: Nil
    }

  implicit def applicationInterpreter[R](
      implicit
      t1: STransformer[Service, R],
      t2: STransformer[Deployment, R]
    ): Interpreter[Application, Namespace, R] = (obj: Application, ns: Namespace) => {
    val service = subinterpreter.serviceInterpreter(obj, ns)
    val deployment = subinterpreter.deploymentInterpreter(obj, ns)

    SSupport(service) :: SSupport(deployment) :: Nil
  }

  implicit def gatewayInterpreter[R](
      implicit
      t: STransformer[Ingress, R]
    ): Interpreter[Gateway, Namespace, R] = (obj: Gateway, ns: Namespace) => {
    val ing = Ingress(
      apiVersion = "networking.k8s.io/v1beta1", // Skuber uses wrong api version
      metadata = subinterpreter.objectMetaInterpreter(obj, ns),
      spec = obj.protocols match {
        case Protocols.Any => None
        case Protocols.Selected(layers) =>
          Some(
            Ingress.Spec(
              rules = layers.map {
                case Protocol.AnyLayers => Ingress.Rule(host = None, http = Ingress.HttpRule())
                case Protocol.SomeLayers(http: HTTP, tcp: TCP, _) =>
                  Ingress.Rule(
                    host = http.host.get,
                    http = Ingress.HttpRule(
                      paths = List(
                        Ingress.Path(
                          http.path.get.getOrElse("/"),
                          Ingress.Backend(
                            serviceName = "???", // FIXME get from identity
                            servicePort = tcp.port.get.get._1 // FIXME
                          )
                        )
                      )
                    )
                  )
              }.toList
              // TODO TLS
            )
          )
      }
    )
    SSupport(ing) :: Nil
  }

  implicit def connectionInterpreter[R](
      implicit
      t: STransformer[NetworkPolicy, R]
    ): Interpreter[Connection, Namespace, R] = (obj: Connection, ns: Namespace) => {
    val netpol = NetworkPolicy(
      metadata = subinterpreter.objectMetaInterpreter(obj, ns),
      spec = Some(
        Spec(
          podSelector = obj.resourceSelector match {
            // NoSelector and AllowSelector are interchangeable here
            case s: Selector =>
              LabelSelector(
                subinterpreter.expressions(s.expressions): _*
              )
          },
          ingress = obj.ingress match {
            case NoSelector    => List()
            case DenySelector  => List() // the difference is in 'policyTypes'
            case AllowSelector => List(IngressRule())
            case s: ApplicationSelector =>
              List(
                IngressRule(
                  from = List(
                    Peer(
                      podSelector = Some(
                        LabelSelector(
                          subinterpreter.expressions(s.expressions): _*
                        )
                      ),
                      namespaceSelector = None,
                      ipBlock = None
                    )
                    // TODO multiple "Peers", combined with logical OR
                  ),
                  ports = subinterpreter.ports(s.protocols)
                )
                // TODO multiple "Rules", combined with logical OR
              )
            case s: NamespaceSelector =>
              List(
                IngressRule(
                  from = List(
                    Peer(
                      podSelector = None,
                      namespaceSelector = Some(
                        LabelSelector(
                          subinterpreter.expressions(s.expressions): _*
                        )
                      ),
                      ipBlock = None
                    )
                    // TODO multiple "Peers", combined with logical OR
                  ),
                  ports = subinterpreter.ports(s.protocols)
                )
                // TODO multiple "Rules", combined with logical OR
              )
            case s: SelectedIPs =>
              List(
                IngressRule(
                  from = subinterpreter.ipBlocks(s),
                  ports = subinterpreter.ports(s.protocols)
                )
              )
            case s: SelectedIPsAndPorts =>
              List(
                IngressRule(
                  from = subinterpreter.ipBlocks(s),
                  ports = subinterpreter.ports(s.protocols)
                )
                // TODO multiple "Rules", combined with logical OR
              )
          },
          egress = obj.egress match {
            case NoSelector    => List()
            case DenySelector  => List() // the difference is in 'policyTypes'
            case AllowSelector => List(EgressRule())
            case s: ApplicationSelector =>
              List(
                EgressRule(
                  to = List(
                    Peer(
                      podSelector = Some(
                        LabelSelector(
                          subinterpreter.expressions(s.expressions): _*
                        )
                      ),
                      namespaceSelector = None,
                      ipBlock = None
                    )
                    // TODO multiple "Peers", combined with logical OR
                  ),
                  ports = subinterpreter.ports(s.protocols)
                )
                // TODO multiple "Rules", combined with logical OR
              )
            case s: NamespaceSelector =>
              List(
                EgressRule(
                  to = List(
                    Peer(
                      podSelector = None,
                      namespaceSelector = Some(
                        LabelSelector(
                          subinterpreter.expressions(s.expressions): _*
                        )
                      ),
                      ipBlock = None
                    )
                    // TODO multiple "Peers", combined with logical OR
                  ),
                  ports = subinterpreter.ports(s.protocols)
                )
                // TODO multiple "Rules", combined with logical OR
              )
            case s: SelectedIPs =>
              List(
                EgressRule(
                  to = subinterpreter.ipBlocks(s),
                  ports = subinterpreter.ports(s.protocols)
                )
                // TODO multiple "Rules", combined with logical OR
              )
            case s: SelectedIPsAndPorts =>
              List(
                EgressRule(
                  to = subinterpreter.ipBlocks(s),
                  ports = subinterpreter.ports(s.protocols)
                )
                // TODO multiple "Rules", combined with logical OR
              )
          },
          policyTypes = (obj.ingress, obj.egress) match {
            case (NoSelector, NoSelector) => List()
            case (_, NoSelector)          => List("Ingress")
            case (NoSelector, _)          => List("Egress")
            case (_, _)                   => List("Ingress", "Egress")
          }
        )
      )
    )

    SSupport(netpol) :: Nil
  }

  object subinterpreter {
    def objectMetaInterpreter(obj: Labeled with Named, ns: Namespace): ObjectMeta =
      ObjectMeta(
        name = obj.name,
        namespace = ns.name,
        labels = obj.labels.asMap
      )

    def serviceInterpreter(obj: Application, ns: Namespace): Service = {
      val svc = Service(
        metadata = subinterpreter.objectMetaInterpreter(obj, ns),
        spec = Some(
          Service.Spec(
            selector = obj.labels.asMap
          )
        )
      )
      obj.ports
        .foldLeft(svc) {
          case (svc, NamedPort(name, number)) =>
            svc.exposeOnPort(
              Service.Port(
                name = name,
                port = number,
                targetPort = Some(Left(number))
              )
            )
          case (svc, APort(number)) =>
            svc.exposeOnPort(
              Service.Port(
                port = number,
                targetPort = Some(Left(number))
              )
            )
          case (svc, _) => svc
        }
    }

    def deploymentInterpreter(obj: Application, ns: Namespace): Deployment = {
      val env = obj.envs.map { env =>
        EnvVar(env.key, EnvVar.StringValue(env.value))
      }

      val mounts = obj.mounts.map(mountInterpreter)

      val container = Container(
        name = obj.name,
        image = obj.image,
        command = obj.command,
        args = obj.args,
        env = env,
        ports = obj.ports.flatMap {
          case NamedPort(name, number) => Container.Port(containerPort = number, name = name) :: Nil
          case APort(number)           => Container.Port(containerPort = number) :: Nil
          case _                       => Nil
        },
        livenessProbe = obj.ping.map(ping => Probe(action = HTTPGetAction(ping.url))),
        readinessProbe = obj.healthCheck.map(
          healthCheck => Probe(action = HTTPGetAction(healthCheck.url))
        ),
        volumeMounts = mounts.map(_._2)
      )

      val dplSpec = Deployment.Spec(
        selector = LabelSelector(
          obj.labels.map(l => LabelSelector.IsEqualRequirement(l.key, l.value)): _*
        ),
        template = Pod.Template
          .Spec(
            spec = Some(
              Pod.Spec(
                containers = container :: Nil,
                volumes = mounts.map(_._1)
              )
            )
          )
          .addLabels(
            obj.labels.asMap
          )
      )

      Deployment(
        metadata = subinterpreter.objectMetaInterpreter(obj, ns),
        spec = Some(dplSpec)
      )
    }

    def mountInterpreter(mount: Mount): (Volume, Volume.Mount) = mount match {
      case KeyValueMount(name, key, mountPath, source) =>
        val volume = Volume(name, source)
        val volumeMount = Volume.Mount(name = name, mountPath = mountPath.toString, subPath = key)
        volume -> volumeMount
    }

    def ipBlocks(s: CIDRs): List[Peer] = {
      s.ips.map {
        case IP.RangeWithExceptions(ip, mask, exceptions) =>
          Peer(
            podSelector = None,
            namespaceSelector = None,
            ipBlock = Some(IPBlock(s"$ip/$mask", exceptions.map(e => s"${e.ip}/${e.mask}").toList))
          )
        case cidr: IP.CIDR =>
          Peer(
            podSelector = None,
            namespaceSelector = None,
            ipBlock = Some(IPBlock(s"${cidr.ip}/${cidr.mask}"))
          )
      }.toList
    }

    object expressions {
      import com.virtuslab.iat.dsl.Expressions._

      def apply(es: Expressions): Seq[LabelSelector.Requirement] =
        apply(es.expressions)

      def apply(es: Set[Expression]): Seq[LabelSelector.Requirement] = {
        es.map {
          case l: Label                => LabelSelector.IsEqualRequirement(l.key, l.value)
          case e: ExistsExpression     => LabelSelector.ExistsRequirement(e.key)
          case e: NotExistsExpression  => LabelSelector.NotExistsRequirement(e.key)
          case e: IsEqualExpression    => LabelSelector.IsEqualRequirement(e.key, e.value)
          case e: IsNotEqualExpression => LabelSelector.IsNotEqualRequirement(e.key, e.value)
          case e: InExpression         => LabelSelector.InRequirement(e.key, e.values.toList)
          case e: NotInExpression      => LabelSelector.NotInRequirement(e.key, e.values.toList)
        }.toSeq
      }
    }

    object ports {
      def apply(ps: Protocols): List[Port] = ps.protocols.flatMap(layer => apply(layer.l4)).toList

      def apply(p: Protocol.L4): Option[Port] = p match {
        case UDP(port: APort)     => Some(Port(Left(port.number), SProtocol.UDP))
        case UDP(port: NamedPort) => Some(Port(Left(port.number), SProtocol.UDP))
        case TCP(port: APort)     => Some(Port(Left(port.number), SProtocol.TCP))
        case TCP(port: NamedPort) => Some(Port(Left(port.number), SProtocol.TCP))
        case _                    => None
      }
    }

  }
}
