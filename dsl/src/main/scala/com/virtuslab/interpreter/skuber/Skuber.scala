package com.virtuslab.interpreter.skuber

import com.virtuslab.dsl._
import com.virtuslab.exporter.skuber.Resource
import com.virtuslab.interpreter.{ Context, Interpreter }
import skuber.apps.v1.Deployment
import skuber.ext.Ingress
import skuber.json.ext.format._
import skuber.json.format._
import skuber.networking.NetworkPolicy
import skuber.networking.NetworkPolicy.{ EgressRule, IPBlock, IngressRule, Peer, Port, Spec }
import skuber.{ ConfigMap, Container, EnvVar, HTTPGetAction, LabelSelector, ObjectMeta, ObjectResource, Pod, Probe, Service, Volume }

object Skuber {

  class SkuberContext extends Context {
    override type Ret[A] = Seq[Resource[_]] // FIXME ?
  }

  implicit val context: SkuberContext = new SkuberContext

  implicit val namespaceInterpreter: Interpreter[SkuberContext, Namespace] =
    (namespace: Definition[SkuberContext, Namespace]) =>
      Seq(
        Resource(
          skuber.Namespace.from(
            ObjectMeta(
              name = namespace.obj.name,
              labels = namespace.obj.labels.toMap
            )
          )
        )
      )

  implicit val configurationInterpreter: Interpreter[SkuberContext, Configuration] =
    (cfg: Definition[SkuberContext, Configuration]) => {
      Seq(
        Resource(
          ConfigMap(
            metadata = ObjectMeta(
              name = cfg.obj.name,
              namespace = cfg.namespace.name,
              labels = cfg.obj.labels.toMap
            ),
            data = cfg.obj.data
          )
        )
      )
    }

  implicit val secretInterpreter: Interpreter[SkuberContext, Secret] =
    (secret: Definition[SkuberContext, Secret]) => {
      Seq(
        Resource(
          skuber.Secret(
            metadata = ObjectMeta(
              name = secret.obj.name,
              namespace = secret.namespace.name,
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

  implicit val applicationInterpreter: Interpreter[SkuberContext, Application] =
    (app: Definition[SkuberContext, Application]) => {
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
        ports = app.obj.ports.map { port =>
          Container.Port(
            containerPort = port.number,
            name = port.name.getOrElse("")
          )
        },
        livenessProbe = app.obj.ping.map(ping => Probe(action = HTTPGetAction(ping.url))),
        readinessProbe = app.obj.healthCheck.map(
          healthCheck => Probe(action = HTTPGetAction(healthCheck.url))
        ),
        volumeMounts = mounts.map(_._2)
      )

      val dpl = deployment(app.namespace, app.obj, container, mounts.map(_._1))
      val svc = service(app.namespace, app.obj)

      Seq(Resource(svc), Resource(dpl))
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
        case (svc, port) =>
          svc.exposeOnPort(
            Service.Port(
              name = port.name.getOrElse(""),
              port = port.number,
              targetPort = Some(Left(port.number))
            )
          )
      }
      .withSelector(
        app.labels.toMap
      )
      .addLabels(
        app.labels.toMap
      )
  }

  implicit val connectionInterpreter: Interpreter[SkuberContext, Connection] =
    (connection: Definition[SkuberContext, Connection]) => {
      Seq(
        Resource(
          NetworkPolicy(
            metadata = ObjectMeta(
              name = connection.obj.name,
              namespace = connection.namespace.name,
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
      case UDP(port) => Some(Port(port.numberOrName, skuber.Protocol.UDP))
      case TCP(port) => Some(Port(port.numberOrName, skuber.Protocol.TCP))
      case _         => None
    }
  }

  implicit val gatewayInterpreter: Interpreter[SkuberContext, Gateway] =
    (gateway: Definition[SkuberContext, Gateway]) => {
      Seq(
        Resource(
          Ingress(
            apiVersion = "networking.k8s.io/v1beta1", // Skuber uses wrong api version
            metadata = ObjectMeta(
              name = gateway.obj.name,
              namespace = gateway.namespace.name,
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
                                  servicePort = tcp.port.numberOrName.left.get // FIXME
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