package com.virtuslab.dsl.interpreter

import cats.syntax.either._
import cats.syntax.option._
import com.virtuslab.dsl.Application.DefinedApplication
import com.virtuslab.dsl.{ Configuration, SystemDef }
import skuber.{ Container, EnvVar, HTTPGetAction, LabelSelector, ObjectMeta, Pod, Probe, Service, Volume }
import skuber.Volume.ConfigMapVolumeSource
import skuber.apps.v1.Deployment

class ApplicationInterpreter(val system: SystemDef, val portForward: PartialFunction[Int, Int] = PartialFunction.empty) {

  protected def generateService(app: DefinedApplication): Service = {
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

  protected def generateDeployment(app: DefinedApplication, container: Container): Deployment = {
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

  def apply(app: DefinedApplication): (Service, Deployment) = {
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
