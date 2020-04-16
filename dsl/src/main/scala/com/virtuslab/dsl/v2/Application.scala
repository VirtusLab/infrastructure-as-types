package com.virtuslab.dsl.v2

import com.virtuslab.json.json4s.jackson.JsonMethods
import magnolia._
import org.json4s.Formats
import org.json4s.JsonAST.JValue

import scala.language.experimental.macros

object openApi {
  import com.virtuslab.kubernetes.client.openapi.model
  implicit val formats: Formats = JsonMethods.defaultFormats

  implicit val namespaceInterpreter: Interpreter[Namespace] = (obj: Namespace) => {
    val namespace = model.Namespace(
      metadata = Some(
        model.ObjectMeta(
          name = Some(obj.name)
        )
      )
    )

    Seq(JsonMethods.asJValue(namespace))
  }

  implicit val configurationInterpreter: Interpreter[Configuration] = (obj: Configuration) => {
    val meta = model.ObjectMeta(
      name = Some(obj.name),
      namespace = Some(obj.namespace.name)
    )

    val configMap = model.ConfigMap(metadata = Some(meta), data = Some(obj.data))
    Seq(JsonMethods.asJValue(configMap))
  }

  implicit val secretInterpreter: Interpreter[Secret] = (obj: Secret) => {
    val meta = model.ObjectMeta(name = Some(obj.name), namespace = Some(obj.namespace.name))
    val data = obj.data.view.mapValues(_.getBytes).toMap

    val secret = model.Secret(metadata = Some(meta), data = Some(data))
    Seq(JsonMethods.asJValue(secret))
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

    Seq(JsonMethods.asJValue(service), JsonMethods.asJValue(deployment))
  }

//  implicit val fooInter: Interpreter[Bar.type] = (obj: Bar.type) => {
//    println("foo")
//    Seq(JsonMethods.asJValue(null))
//  }
}

trait Interpreter[A] {
  def interpret(obj: A): Seq[JValue]
}

object Interpreter {
  type Typeclass[T] = Interpreter[T]

  def combine[T](ctx: CaseClass[Interpreter, T]): Interpreter[T] = (obj: T) => {
    ctx.parameters.flatMap { p =>
      p.typeclass.interpret(p.dereference(obj))
    }
  }

  def dispatch[T](ctx: SealedTrait[Interpreter, T]): Interpreter[T] = (obj: T) => {
    ctx.dispatch(obj) { sub =>
      sub.typeclass.interpret(sub.cast(obj))
    }
  }

  implicit def gen[T]: Interpreter[T] = macro Magnolia.gen[T]

  def apply[T](implicit a: Interpreter[T]): Typeclass[T] = a
}

final case class Namespace(name: String)

case class Configuration(
    name: String,
    namespace: Namespace,
    data: Map[String, String])

case class Secret(
    name: String,
    namespace: Namespace,
    data: Map[String, String])

sealed trait Foo
case object Bar extends Foo

case class Application(
    name: String,
    namespace: Namespace,
    configurations: List[Configuration] = Nil,
    secrets: List[Secret] = Nil)

object Test extends App {

  import openApi._

  private val namespace: Namespace = Namespace("foo")

  case class MyNamespaceDef(
      namespace: Namespace = namespace,
      foo: Foo = Bar,
      superApp: Application = Application(name = "bar", namespace = namespace),
      myConfiguration: Configuration = Configuration(name = "config-foo", namespace = namespace, data = Map.empty),
      mySecret: Secret = Secret("config-foo", namespace = namespace, data = Map.empty))

  val myNs = MyNamespaceDef()

  println(Interpreter[MyNamespaceDef].interpret(myNs))
}
