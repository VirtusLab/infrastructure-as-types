package com.virtuslab.iat.skuber

import _root_.skuber.apps.v1.Deployment
import _root_.skuber.networking.NetworkPolicy.{ IPBlock, Peer, Port => SPort }
import _root_.skuber.{ EnvVar, LabelSelector, ObjectMeta, Pod, Service, Volume, Container => SContainer, Protocol => SProtocol }
import com.virtuslab.iat.dsl.Port._
import com.virtuslab.iat.dsl._
import com.virtuslab.iat.kubernetes.dsl._

trait DefaultSubinterpreters {
  import Label.ops._

  def objectMetaInterpreter(obj: Labeled with Named, ns: Namespace): ObjectMeta =
    ObjectMeta(
      name = obj.name,
      namespace = ns.name,
      labels = obj.labels.asMap
    )

  def serviceInterpreter(obj: Application, ns: Namespace): Service = {
    val svc = Service(
      metadata = objectMetaInterpreter(obj, ns),
      spec = Some(
        Service.Spec(
          selector = obj.labels.asMap
        )
      )
    )
    obj.allPorts
      .map(_.port)
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
    val mounts = obj.mounts.map(mountInterpreter)

    val dplSpec = Deployment.Spec(
      selector = LabelSelector(
        obj.labels.map(l => LabelSelector.IsEqualRequirement(l.key, l.value)): _*
      ),
      template = Pod.Template
        .Spec(
          spec = Some(
            Pod.Spec(
              containers = obj.containers.map(c => containerInterpreter(c, mounts)),
              volumes = mounts.map(_._1)
            )
          )
        )
        .addLabels(
          obj.labels.asMap
        )
    )

    Deployment(
      metadata = objectMetaInterpreter(obj, ns),
      spec = Some(dplSpec)
    )
  }

  def containerInterpreter(c: Container, mounts: List[(Volume, Volume.Mount)]): SContainer = {
    SContainer(
      name = c.name,
      image = c.image,
      command = c.command,
      args = c.args,
      env = c.envs.map { env =>
        EnvVar(env._1, EnvVar.StringValue(env._2))
      },
      ports = c.ports.map(_.port).flatMap {
        case NamedPort(name, number) => SContainer.Port(containerPort = number, name = name) :: Nil
        case APort(number)           => SContainer.Port(containerPort = number) :: Nil
        case _                       => Nil
      },
      volumeMounts = mounts.map(_._2)
    )
  }

  def mountInterpreter(mount: Mount): (Volume, Volume.Mount) = mount match {
    case KeyValueMount(name, key, mountPath, source: Volume.Source) =>
      val volume = Volume(name, source)
      val volumeMount = Volume.Mount(name = name, mountPath = mountPath.toString, subPath = key)
      volume -> volumeMount
    case _ => ??? // FIXME
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
    def apply(ps: Protocols): List[SPort] = ps.protocols.flatMap(layer => apply(layer.l4)).toList

    def apply(p: Protocol.L4): Option[SPort] = p match {
      case UDP(port: APort)     => Some(SPort(Left(port.number), SProtocol.UDP))
      case UDP(port: NamedPort) => Some(SPort(Left(port.number), SProtocol.UDP))
      case TCP(port: APort)     => Some(SPort(Left(port.number), SProtocol.TCP))
      case TCP(port: NamedPort) => Some(SPort(Left(port.number), SProtocol.TCP))
      case _                    => None
    }
  }

}
