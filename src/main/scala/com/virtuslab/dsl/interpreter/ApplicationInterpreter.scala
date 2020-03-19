package com.virtuslab.dsl.interpreter

import cats.syntax.either._
import cats.syntax.option._
import com.virtuslab.dsl.Application.ApplicationDefinition
import com.virtuslab.dsl.{ Configuration, DistributedSystem }
import skuber.Volume.ConfigMapVolumeSource
import skuber.apps.v1.Deployment
import skuber.{ Container, EnvVar, HTTPGetAction, LabelSelector, ObjectMeta, Pod, Probe, Service, Volume }

class ApplicationInterpreter(val system: DistributedSystem, val portForward: PartialFunction[Int, Int] = PartialFunction.empty) {

  protected def generateService(app: ApplicationDefinition): Service = {
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
        app.labels.map(l => l.name -> l.value).toMap
      )
      .addLabels(
        app.labels.map(l => l.name -> l.value).toMap
      )
  }

  protected def volumeName(configuration: Configuration): String = {
    s"config-${configuration.name}"
  }

  protected def generateDeployment(app: ApplicationDefinition, container: Container): Deployment = {
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
        app.labels.map(l => l.name -> l.value).toMap
      )
    val dplSpec = Deployment.Spec(
      selector = LabelSelector(
        app.labels.map(l => LabelSelector.IsEqualRequirement(l.name, l.value)).toSeq: _*
      ),
      template = podTemplateSpec
    )

    Deployment(
      metadata = ObjectMeta(
        name = app.name,
        namespace = app.namespace.name,
        labels = app.labels.map(l => l.name -> l.value).toMap
      ),
      spec = dplSpec.some
    )
  }

  def apply(app: ApplicationDefinition): (Service, Deployment) = {
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
