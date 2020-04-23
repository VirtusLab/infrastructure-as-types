package com.virtuslab.iat.kubernetes

import com.virtuslab.iat.core.Transformable.Transformer
import com.virtuslab.iat.core.{ RootInterpreter, _ }
import com.virtuslab.iat.dsl._
import com.virtuslab.kubernetes.client.openapi.model.{ Deployment, ObjectMeta, Service }

object openapi {
  import com.virtuslab.iat.json.json4s.{ JValueMetadataExtractor, JValueTransformable }

  object json4s extends JValueTransformable with JValueMetadataExtractor {
    import com.virtuslab.iat.json.json4s.jackson.{ JsonMethods, YamlMethods }
    import org.json4s.JValue

    object InterpreterDerivation extends InterpreterDerivation[Namespace, JValue]

    def asMetaJValue(
        js: Seq[JValue]
      )(implicit
        transformer: Transformer[JValue, Either[String, Metadata]]
      ): Iterable[(Metadata, JValue)] = {
      val mt = js
        .map(transformer)
        .map(_.transform)
        .map(
          _.fold(
            e =>
              throw new IllegalStateException( /* FIXME: how to make it compile-time? */
                s"metadata extraction failed: $e\nJson:\n${js.map(JsonMethods.pretty)}\n"
              ),
            v => v
          )
        )
      mt.zip(js)
    }

    def asMetaJsonString(js: Seq[JValue]): Iterable[(Metadata, String)] =
      asMetaJValue(js).map(e => e._1 -> JsonMethods.pretty(e._2))
    def asMetaYamlString(js: Seq[JValue]): Iterable[(Metadata, String)] =
      asMetaJValue(js).map(e => e._1 -> YamlMethods.pretty(e._2))

    def asJValue[A](a: A)(implicit t: Transformer[A, JValue]): JValue = t(a).transform
    def asJsonString[A](a: A)(implicit t: Transformer[A, JValue]): String = JsonMethods.pretty(asJValue(a))
    def asYamlString[A](a: A)(implicit t: Transformer[A, JValue]): String = YamlMethods.pretty(asJValue(a))
  }

  import Label.ops._
  import Secret.ops._
  import com.virtuslab.kubernetes.client.openapi.model

  def interpret[A, R](obj: A)(implicit i: RootInterpreter[A, R]): List[R] = Interpreter.interpret(obj)
  def interpret[A, C, R](obj: A, ctx: C)(implicit i: Interpreter[A, C, R]): List[R] = Interpreter.interpret(obj, ctx)

  implicit def namespaceInterpreter[R](
      implicit
      t: Transformer[model.Namespace, R]
    ): RootInterpreter[Namespace, R] =
    (obj: Namespace) => {
      val namespace = model.Namespace(
        apiVersion = Some("v1"),
        kind = Some("Namespace"),
        metadata = Some(
          model.ObjectMeta(
            name = Some(obj.name),
            labels = Some(obj.labels.asMap)
          )
        )
      )

      Support(namespace) :: Nil
    }

  implicit def configurationInterpreter[R](
      implicit
      t: Transformer[model.ConfigMap, R]
    ): Interpreter[Configuration, Namespace, R] = (obj: Configuration, ns: Namespace) => {
    val config = model.ConfigMap(
      apiVersion = Some("v1"),
      kind = Some("ConfigMap"),
      metadata = Some(
        model.ObjectMeta(
          name = Some(obj.name),
          namespace = Some(ns.name),
          labels = Some(obj.labels.asMap)
        )
      ),
      data = Some(obj.data)
    )

    Support(config) :: Nil
  }

  implicit def secretInterpreter[R](
      implicit
      t: Transformer[model.Secret, R]
    ): Interpreter[Secret, Namespace, R] =
    (obj: Secret, ns: Namespace) => {
      val secret = model.Secret(
        apiVersion = Some("v1"),
        kind = Some("Secret"),
        metadata = Some(
          model.ObjectMeta(
            name = Some(obj.name),
            namespace = Some(ns.name),
            labels = Some(obj.labels.asMap)
          )
        ),
        data = Some(obj.data.view.mapValues(base64encode).toMap)
      )

      Support(secret) :: Nil
    }

  implicit def applicationInterpreter[R](
      implicit
      t1: Transformer[model.Service, R],
      t2: Transformer[model.Deployment, R]
    ): Interpreter[Application, Namespace, R] = (obj: Application, ns: Namespace) => {
    val meta = model.ObjectMeta(
      name = Some(obj.name),
      namespace = Some(ns.name),
      labels = Some(obj.labels.asMap)
    )

    val service = subinterpreter.applicationServiceInterpreter(meta)
    val deployment = subinterpreter.applicationDeploymentInterpreter(meta, obj)

    Support(service) :: Support(deployment) :: Nil
  }

  implicit def gatewayInterpreter[R](
      implicit
      t: Transformer[model.Ingress, R]
    ): Interpreter[Gateway, Namespace, R] =
    (obj: Gateway, ns: Namespace) => {
      val ingress = model.Ingress(
        apiVersion = Some("networking.k8s.io/v1beta1"),
        kind = Some("Ingress"),
        metadata = Some(
          model.ObjectMeta(
            name = Some(obj.name),
            namespace = Some(ns.name),
            labels = Some(obj.labels.asMap)
          )
        )
      )
      Support(ingress) :: Nil
    }

  implicit def labelInterpreter[R](
      implicit
      t: Transformer[(String, String), R]
    ): RootInterpreter[Label, R] =
    (l: Label) => {
      Support(l.asTuple) :: Nil
    }

  object subinterpreter {
    def applicationServiceInterpreter(meta: model.ObjectMeta): Service = {
      model.Service(
        apiVersion = Some("v1"),
        kind = Some("Service"),
        metadata = Some(meta),
        spec = Some(model.ServiceSpec())
      )
    }

    def applicationDeploymentInterpreter(meta: ObjectMeta, obj: Application): Deployment = {
      model.Deployment(
        apiVersion = Some("apps/v1"),
        kind = Some("Deployment"),
        metadata = Some(meta),
        spec = Some(
          model.DeploymentSpec(
            template = model.PodTemplateSpec(
              spec = Some(
                model.PodSpec(containers = Seq(model.Container(name = obj.name)))
              )
            )
          )
        )
      )
    }

  }
}
