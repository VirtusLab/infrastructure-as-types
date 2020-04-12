package com.virtuslab.interpreter.skuber

import com.virtuslab.dsl.Port.{ APort, NamedPort }
import com.virtuslab.dsl._
import com.virtuslab.exporter.skuber.Resource
import com.virtuslab.interpreter.{ Context, Interpreter, RootInterpreter }
import skuber.apps.v1.Deployment
import skuber.ext.Ingress
import skuber.json.ext.format._
import skuber.json.format._
import skuber.networking.NetworkPolicy
import skuber.networking.NetworkPolicy.{ EgressRule, IPBlock, IngressRule, Peer, Port, Spec }
import skuber.{ ConfigMap, Container, EnvVar, HTTPGetAction, LabelSelector, ObjectMeta, ObjectResource, Pod, Probe, Service, Volume }

object Skuber {

  class SkuberContext extends Context {
    override type Ret = Resource[ObjectResource] // FIXME ?
  }

  implicit val context: SkuberContext = new SkuberContext

  implicit val systemInterpreter: RootInterpreter[SkuberContext, DistributedSystem, Namespace] =
    (_: RootDefinition[SkuberContext, DistributedSystem, Namespace]) => Seq()

  implicit val namespaceInterpreter: Interpreter[SkuberContext, DistributedSystem, Namespace, Labeled] =
    (namespace: Definition[SkuberContext, DistributedSystem, Namespace, Labeled]) =>
      Seq(
        Resource.weak(
          skuber.Namespace.from(
            ObjectMeta(
              name = namespace.obj.name,
              labels = namespace.obj.labels.toMap
            )
          )
        )
      )

  implicit val configurationInterpreter: Interpreter[SkuberContext, Namespace, Configuration, Labeled] =
    (cfg: Definition[SkuberContext, Namespace, Configuration, Labeled]) => {
      Seq(
        Resource.weak(
          ConfigMap(
            metadata = ObjectMeta(
              name = cfg.obj.name,
              namespace = cfg.holder.name,
              labels = cfg.obj.labels.toMap
            ),
            data = cfg.obj.data
          )
        )
      )
    }

  implicit val secretInterpreter: Interpreter[SkuberContext, Namespace, Secret, Labeled] =
    (secret: Definition[SkuberContext, Namespace, Secret, Labeled]) => {
      Seq(
        Resource.weak(
          skuber.Secret(
            metadata = ObjectMeta(
              name = secret.obj.name,
              namespace = secret.holder.name,
              labels = secret.obj.labels.toMap
            ),
            data = secret.obj.data.view.mapValues(_.getBytes).toMap
          )
        )
      )
    }

  private[skuber] def mount(mount: Mount): (Volume, Volume.Mount) = mount match {
    case KeyValueMount(name, key, mountPath, source) =>
      val volume = Volume(name, source)
      val volumeMount = Volume.Mount(name = name, mountPath = mountPath.toString, subPath = key)
      volume -> volumeMount
  }

  implicit val applicationInterpreter: Interpreter[SkuberContext, Namespace, Application, Labeled] =
    (app: Definition[SkuberContext, Namespace, Application, Labeled]) => {
      val env = app.obj.envs.map { env =>
        EnvVar(env.key, EnvVar.StringValue(env.value))
      }

      val mounts = app.obj.mounts.map(mount)

      val container = Container(
        name = app.obj.name,
        image = app.obj.image,
        command = app.obj.command,
        args = app.obj.args,
        env = env,
        ports = app.obj.ports.flatMap {
          case NamedPort(name, number) => Container.Port(containerPort = number, name = name) :: Nil
          case APort(number)           => Container.Port(containerPort = number) :: Nil
          case _                       => Nil
        },
        livenessProbe = app.obj.ping.map(ping => Probe(action = HTTPGetAction(ping.url))),
        readinessProbe = app.obj.healthCheck.map(
          healthCheck => Probe(action = HTTPGetAction(healthCheck.url))
        ),
        volumeMounts = mounts.map(_._2)
      )

      val dpl = deployment(app.holder, app.obj, container, mounts.map(_._1))
      val svc = service(app.holder, app.obj)

      Seq(Resource.weak(svc), Resource.weak(dpl))
    }

  private def deployment(
      ns: Namespace,
      app: Application,
      container: Container,
      mountedVolumes: List[Volume]
    ): Deployment = {
    val podSpec = Pod.Spec(
      containers = container :: Nil,
      volumes = mountedVolumes
    )
    val podTemplateSpec = Pod.Template
      .Spec(
        spec = Some(podSpec)
      )
      .addLabels(
        app.labels.toMap
      )
    val dplSpec = Deployment.Spec(
      selector = LabelSelector(
        app.labels.map(l => LabelSelector.IsEqualRequirement(l.key, l.value)).toSeq: _*
      ),
      template = podTemplateSpec
    )

    Deployment(
      metadata = ObjectMeta(
        name = app.name,
        namespace = ns.name,
        labels = app.labels.toMap
      ),
      spec = Some(dplSpec)
    )
  }

  private def service(ns: Namespace, app: Application): Service = {
    app.ports
      .foldLeft(Service(metadata = ObjectMeta(name = app.name, namespace = ns.name))) {
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
      .withSelector(
        app.labels.toMap
      )
      .addLabels(
        app.labels.toMap
      )
  }

  implicit val connectionInterpreter: Interpreter[SkuberContext, Namespace, Connection, Labeled] =
    (connection: Definition[SkuberContext, Namespace, Connection, Labeled]) => {
      Seq(
        Resource.weak(
          NetworkPolicy(
            metadata = ObjectMeta(
              name = connection.obj.name,
              namespace = connection.holder.name,
              labels = connection.obj.labels.toMap
            ),
            spec = Some(
              Spec(
                podSelector = connection.obj.resourceSelector match {
                  // NoSelector and AllowSelector are interchangeable here
                  case s: Selector =>
                    LabelSelector(
                      expressions(s.expressions): _*
                    )
                },
                ingress = connection.obj.ingress match {
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
                                expressions(s.expressions): _*
                              )
                            ),
                            namespaceSelector = None,
                            ipBlock = None
                          )
                          // TODO multiple "Peers", combined with logical OR
                        ),
                        ports = ports(s.protocols)
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
                                expressions(s.expressions): _*
                              )
                            ),
                            ipBlock = None
                          )
                          // TODO multiple "Peers", combined with logical OR
                        ),
                        ports = ports(s.protocols)
                      )
                      // TODO multiple "Rules", combined with logical OR
                    )
                  case s: SelectedIPs =>
                    List(
                      IngressRule(
                        from = ipBlocks(s),
                        ports = ports(s.protocols)
                      )
                    )
                  case s: SelectedIPsAndPorts =>
                    List(
                      IngressRule(
                        from = ipBlocks(s),
                        ports = ports(s.protocols)
                      )
                      // TODO multiple "Rules", combined with logical OR
                    )
                },
                egress = connection.obj.egress match {
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
                                expressions(s.expressions): _*
                              )
                            ),
                            namespaceSelector = None,
                            ipBlock = None
                          )
                          // TODO multiple "Peers", combined with logical OR
                        ),
                        ports = ports(s.protocols)
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
                                expressions(s.expressions): _*
                              )
                            ),
                            ipBlock = None
                          )
                          // TODO multiple "Peers", combined with logical OR
                        ),
                        ports = ports(s.protocols)
                      )
                      // TODO multiple "Rules", combined with logical OR
                    )
                  case s: SelectedIPs =>
                    List(
                      EgressRule(
                        to = ipBlocks(s),
                        ports = ports(s.protocols)
                      )
                      // TODO multiple "Rules", combined with logical OR
                    )
                  case s: SelectedIPsAndPorts =>
                    List(
                      EgressRule(
                        to = ipBlocks(s),
                        ports = ports(s.protocols)
                      )
                      // TODO multiple "Rules", combined with logical OR
                    )
                },
                policyTypes = (connection.obj.ingress, connection.obj.egress) match {
                  case (NoSelector, NoSelector) => List()
                  case (_, NoSelector)          => List("Ingress")
                  case (NoSelector, _)          => List("Egress")
                  case (_, _)                   => List("Ingress", "Egress")
                }
              )
            )
          )
        )
      )
    }

  private def ipBlocks(s: CIDRs): List[Peer] = {
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
    import com.virtuslab.dsl.Expressions._

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
      case UDP(port: APort)     => Some(Port(Left(port.number), skuber.Protocol.UDP))
      case UDP(port: NamedPort) => Some(Port(Left(port.number), skuber.Protocol.UDP))
      case TCP(port: APort)     => Some(Port(Left(port.number), skuber.Protocol.TCP))
      case TCP(port: NamedPort) => Some(Port(Left(port.number), skuber.Protocol.TCP))
      case _                    => None
    }
  }

  implicit val gatewayInterpreter: Interpreter[SkuberContext, Namespace, Gateway, Labeled] =
    (gateway: Definition[SkuberContext, Namespace, Gateway, Labeled]) => {
      Seq(
        Resource.weak(
          Ingress(
            apiVersion = "networking.k8s.io/v1beta1", // Skuber uses wrong api version
            metadata = ObjectMeta(
              name = gateway.obj.name,
              namespace = gateway.holder.name,
              labels = gateway.obj.labels.toMap
            ),
            spec = gateway.obj.protocols match {
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
        )
      )
    }
}
