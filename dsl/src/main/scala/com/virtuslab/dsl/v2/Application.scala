package com.virtuslab.dsl.v2

import com.virtuslab.kubernetes.client.openapi.core.ApiModel
import magnolia._

import scala.language.experimental.macros

trait Interpreter[A] {
  def interpret(obj: A): Seq[ApiModel]
}

final case class Namespace(name: String)

trait NamespaceDef {
  def namespace: Namespace
}

case class Configuration(
    name: String,
    namespace: Namespace,
    data: Map[String, String])

case class Secret(
    name: String,
    namespace: Namespace,
    data: Map[String, String])

case class Application(
    name: String,
    namespace: Namespace,
    configurations: List[Configuration] = Nil,
    secrets: List[Secret] = Nil)

object Test {

  object openApi {
    import com.virtuslab.kubernetes.client.openapi.model

    implicit val namespaceInterpreter: Interpreter[Namespace] = (obj: Namespace) => {
      Seq(
        model.Namespace(
          metadata = Some(
            model.ObjectMeta(
              name = Some(obj.name)
            )
          )
        )
      )
    }

    implicit val configurationInterpreter: Interpreter[Configuration] = (obj: Configuration) => {
      val meta = model.ObjectMeta(
        name = Some(obj.name),
        namespace = Some(obj.namespace.name)
      )

      Seq(model.ConfigMap(metadata = Some(meta), data = Some(obj.data)))
    }

    implicit val secretInterpreter: Interpreter[Secret] = (obj: Secret) => {
      val meta = model.ObjectMeta(name = Some(obj.name), namespace = Some(obj.namespace.name))
      val data = obj.data.view.mapValues(_.getBytes).toMap

      Seq(model.Secret(metadata = Some(meta), data = Some(data)))
    }

    implicit val applicationInterpreter: Interpreter[Application] = (obj: Application) => {
      val meta = model.ObjectMeta(name = Some(obj.name), namespace = Some(obj.namespace.name))

      val container = model.Container(name = obj.name)
      val podSpec = model.PodSpec(containers = Seq(container))
      val podTemplateSpec = model.PodTemplateSpec(spec = Some(podSpec))
      val deploymentSpec = model.DeploymentSpec(template = podTemplateSpec)
      val deployment = model.Deployment(metadata = Some(meta), spec = Some(deploymentSpec))

      val serviceSpec = model.ServiceSpec()
      val service = model.Service(metadata = Some(meta), spec = Some(serviceSpec))

      Seq(service, deployment)
    }
  }

  case class MyNamespace(
      namespace: Namespace,
      superApp: Application,
      myConfiguration: Configuration,
      mySecret: Secret)
    extends NamespaceDef
}
