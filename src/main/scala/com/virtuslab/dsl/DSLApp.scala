package com.virtuslab.dsl

import cats.syntax.either._
import cats.syntax.option._
import skuber.Volume.ConfigMapVolumeSource
import skuber.apps.v1.Deployment
import skuber.{ Container, EnvVar, HTTPGetAction, LabelSelector, ObjectMeta, ObjectResource, Pod, Probe, Service, Volume }

trait ApplicationInterpreter[A <: Application] {
  def system: System

  def portForward: PartialFunction[Int, Int]

  protected def generateService(app: A): Service = {
    app.ports
      .foldLeft(Service(app.name)) {
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

  protected def generateDeployment(app: A, container: Container): Deployment = {
    val podSpec = Pod.Spec(
      containers = container :: Nil,
      // FIXME --start--
      volumes = List(
        Volume("config", ConfigMapVolumeSource(name = "app"))
      )
      // FIXME --end--
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
      metadata = ObjectMeta(name = app.name),
      spec = dplSpec.some
    )
  }

  def apply(app: A): (Service, Deployment)
}

class HttpApplicationInterpreter(val system: System, val portForward: PartialFunction[Int, Int] = PartialFunction.empty)
  extends ApplicationInterpreter[HttpApplication] {

  def apply(app: HttpApplication): (Service, Deployment) = {
    val svc = generateService(app)

    val env = app.envs.map { env =>
      EnvVar(env.key, EnvVar.StringValue(env.value))
    }
    val container = Container(
      name = app.name,
      image = app.image,
      // FIXME --start--
      command = List("cloud-file-server"),
      args = List("--config", "/opt/config.yaml"),
      // FIXME --end--
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
      // FIXME --start--
      volumeMounts = List(
        Volume.Mount(
          name = "config",
          mountPath = "/opt/",
          readOnly = true
        )
      )
      // FIXME --end--
    )

    val dpl = generateDeployment(app, container)

    (svc, dpl)
  }
}

class SystemInterpreter(
    applicationInterpreters: PartialFunction[
      Application,
      ApplicationInterpreter[Application]
    ]) {

  def apply(system: System): Seq[(Service, Deployment)] = {
    system.applications.map { app =>
      if (applicationInterpreters.isDefinedAt(app)) {
        applicationInterpreters(app)(app)
      } else {
        throw new IllegalArgumentException(
          s"Application $app is not suitable for the interpreter."
        )
      }
    }
  }
}

object SystemInterpreter {
  def apply(
      applicationInterpreters: PartialFunction[
        Application,
        ApplicationInterpreter[Application]
      ]
    ): SystemInterpreter = new SystemInterpreter(applicationInterpreters)

  def of(system: System): SystemInterpreter = {
    val httpApplicationInterpreter = new HttpApplicationInterpreter(system).asInstanceOf[ApplicationInterpreter[Application]] // FIXME
    new SystemInterpreter({
      case _: HttpApplication => httpApplicationInterpreter
    })
  }
}
