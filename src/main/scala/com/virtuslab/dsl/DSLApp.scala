package com.virtuslab.dsl

import cats.syntax.either._
import cats.syntax.option._
import skuber.Volume.ConfigMapVolumeSource
import skuber.apps.v1.Deployment
import skuber.{ ConfigMap, Container, EnvVar, HTTPGetAction, LabelSelector, ObjectMeta, ObjectResource, Pod, Probe, Service, Volume }

class NamespaceInterpreter() {
  def apply(namespace: Namespace): skuber.Namespace = {
    skuber.Namespace.from(ObjectMeta(name = namespace.name))
  }
}

class ConfigurationInterpreter() {
  def apply(configuration: Configuration): ConfigMap = {
    ConfigMap(
      metadata = ObjectMeta(name = configuration.name, namespace = configuration.namespace.name),
      data = configuration.data
    )
  }
}

trait ApplicationInterpreter[A <: Application] {
  def system: System

  def portForward: PartialFunction[Int, Int]

  protected def generateService(app: A): Service = {
    app.ports
      .foldLeft(Service(metadata = ObjectMeta(name = app.name, namespace = app.namespace.name))) {
        case (svc, port) =>
          val rewrittenPort =
            portForward.applyOrElse(port.number, identity[Int])
          val servicePort = Service.Port(
            name = port.name.getOrElse(""),
            port = rewrittenPort,
            targetPort = port.number.asLeft.some
          )
          svc.exposeOnPort(servicePort)
      }
      .withSelector(
        Map(
          "system" -> system.name,
          "app" -> app.name
        )
      )
      .addLabels(
        Map(
          "system" -> system.name,
          "app" -> app.name
        )
      )
  }

  protected def volumeName(configuration: Configuration): String = {
    s"config-${configuration.name}"
  }

  protected def generateDeployment(app: A, container: Container): Deployment = {
    val podSpec = Pod.Spec(
      containers = container :: Nil,
      volumes = app.configurations.map { cfg =>
        Volume(volumeName(cfg), ConfigMapVolumeSource(name = cfg.name))
      }
    )
    val podTemplateSpec = Pod.Template
      .Spec(
        spec = podSpec.some
      )
      .addLabels(
        Map(
          "system" -> system.name,
          "app" -> app.name
        )
      )
    val dplSpec = Deployment.Spec(
      selector = LabelSelector(
        LabelSelector.IsEqualRequirement("system", system.name),
        LabelSelector.IsEqualRequirement("app", app.name)
      ),
      template = podTemplateSpec
    )

    Deployment(
      metadata = ObjectMeta(name = app.name, namespace = app.namespace.name),
      spec = dplSpec.some
    )
  }

  def apply(app: A): (Service, Deployment)
}

class HttpApplicationInterpreter(val system: System, val portForward: PartialFunction[Int, Int] = PartialFunction.empty)
  extends ApplicationInterpreter[Application] {

  def apply(app: Application): (Service, Deployment) = {
    val svc = generateService(app)

    val env = app.envs.map { env =>
      EnvVar(env.key, EnvVar.StringValue(env.value))
    }
    val container = Container(
      name = app.name,
      image = app.image,
      command = app.command,
      args = app.args,
      env = env,
      ports = app.ports.map { port =>
        Container.Port(
          containerPort = port.number,
          name = port.name.getOrElse("")
        )
      },
      livenessProbe = app.ping.map(ping => Probe(action = HTTPGetAction(ping.url))),
      readinessProbe = app.healthCheck.map(
        healthCheck => Probe(action = HTTPGetAction(healthCheck.url))
      ),
      volumeMounts = app.configurations.map { cfg =>
        Volume.Mount(
          name = volumeName(cfg),
          mountPath = "/opt", // TODO
          readOnly = true // TODO
        )
      }
    )

    val dpl = generateDeployment(app, container)

    (svc, dpl)
  }
}

class SystemInterpreter(
    applicationInterpreters: PartialFunction[
      Application,
      ApplicationInterpreter[Application]
    ],
    config: ConfigurationInterpreter,
    namespace: NamespaceInterpreter) {

  def apply(system: System): Seq[ObjectResource] = {
    system.namespaces.flatMap { ns =>
      Seq(namespace(ns)) ++ ns.members.toList.flatMap {
        case app: Application =>
          if (applicationInterpreters.isDefinedAt(app)) {
            val (svc, dpl) = applicationInterpreters(app)(app)
            Seq(svc, dpl)
          } else {
            throw new IllegalArgumentException(
              s"Application $app is not suitable for the interpreter."
            )
          }
        case cfg: Configuration =>
          Seq(config(cfg))
      }
    }
  }
}

object SystemInterpreter {
  def apply(
      applicationInterpreters: PartialFunction[
        Application,
        ApplicationInterpreter[Application]
      ],
      configurationInterpreter: ConfigurationInterpreter,
      namespaceInterpreter: NamespaceInterpreter
    ): SystemInterpreter = new SystemInterpreter(applicationInterpreters, configurationInterpreter, namespaceInterpreter)

  def of(system: System): SystemInterpreter = {
    val httpApplicationInterpreter = new HttpApplicationInterpreter(system).asInstanceOf[ApplicationInterpreter[Application]] // FIXME
    new SystemInterpreter({
      case _: Application => httpApplicationInterpreter
    }, new ConfigurationInterpreter, new NamespaceInterpreter)
  }
}
