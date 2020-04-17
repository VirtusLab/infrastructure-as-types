package com.virtuslab.dsl.v2

import com.virtuslab.json.json4s.jackson.JsonMethods
import magnolia._
import org.json4s.Formats
import org.json4s.JsonAST.JValue

import scala.language.experimental.macros

object openApi {
  import com.virtuslab.kubernetes.client.openapi.model
  implicit val formats: Formats = JsonMethods.defaultFormats

  implicit val namespaceInterpreter: Interpreter[Namespace] = (obj: Namespace, namespace: Namespace) => {
    val namespace = model.Namespace(
      metadata = Some(
        model.ObjectMeta(
          name = Some(obj.name)
        )
      )
    )

    Seq(JsonMethods.asJValue(namespace))
  }

  implicit val configurationInterpreter: Interpreter[Configuration] = (obj: Configuration, namespace: Namespace) => {
    val meta = model.ObjectMeta(
      name = Some(obj.name),
      namespace = Some(namespace.name)
    )

    val configMap = model.ConfigMap(metadata = Some(meta), data = Some(obj.data))
    Seq(JsonMethods.asJValue(configMap))
  }

  implicit val secretInterpreter: Interpreter[Secret] = (obj: Secret, namespace: Namespace) => {
    val meta = model.ObjectMeta(name = Some(obj.name), namespace = Some(namespace.name))
    val data = obj.data.view.mapValues(_.getBytes).toMap

    val secret = model.Secret(metadata = Some(meta), data = Some(data))
    Seq(JsonMethods.asJValue(secret))
  }

  implicit val applicationInterpreter: Interpreter[Application] = (obj: Application, namespace: Namespace) => {
    val meta = model.ObjectMeta(name = Some(obj.name), namespace = Some(namespace.name))

    val container = model.Container(name = obj.name)
    val podSpec = model.PodSpec(containers = Seq(container))
    val podTemplateSpec = model.PodTemplateSpec(spec = Some(podSpec))
    val deploymentSpec = model.DeploymentSpec(template = podTemplateSpec)
    val deployment = model.Deployment(metadata = Some(meta), spec = Some(deploymentSpec))

    val serviceSpec = model.ServiceSpec()
    val service = model.Service(metadata = Some(meta), spec = Some(serviceSpec))

    Seq(JsonMethods.asJValue(service), JsonMethods.asJValue(deployment))
  }

}

trait Interpreter[A] {
  def interpret(obj: A, ns: Namespace): Seq[JValue] //FIXME: change return type
}

object Interpreter {
  type Typeclass[T] = Interpreter[T]

  def combine[T](ctx: CaseClass[Interpreter, T]): Interpreter[T] = (obj: T, namespace: Namespace) => {
    ctx.parameters.flatMap { p =>
      p.typeclass.interpret(p.dereference(obj), namespace)
    }
  }

  def dispatch[T](ctx: SealedTrait[Interpreter, T]): Interpreter[T] = (obj: T, namespace: Namespace) => {
    ctx.dispatch(obj) { sub =>
      sub.typeclass.interpret(sub.cast(obj), namespace)
    }
  }

  def gen[T]: Interpreter[T] = macro Magnolia.gen[T]
}

final case class Namespace(name: String)

case class Configuration(name: String, data: Map[String, String])

case class Secret(name: String, data: Map[String, String])

case class Application(
    name: String,
    configurations: List[Configuration] = Nil,
    secrets: List[Secret] = Nil)

object Test extends App {

  import openApi._

  private val namespace: Namespace = Namespace("foo")

  case class MyDef(
      superApp: Application = Application(name = "bar"),
      myConfiguration: Configuration = Configuration(name = "config-foo", data = Map.empty),
      mySecret: Secret = Secret("config-foo", data = Map.empty))

  val myNs = MyDef()

  def materialize[A: Interpreter](obj: A): Seq[JValue] = {
    Interpreter.gen[MyDef].interpret(myNs, namespace)
  }
}
