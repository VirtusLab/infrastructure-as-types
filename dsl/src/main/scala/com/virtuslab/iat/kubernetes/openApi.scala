package com.virtuslab.iat.kubernetes

import com.virtuslab.iat.core.Transformable.Transformer
import com.virtuslab.iat.core._
import com.virtuslab.iat.dsl.{ Application, Configuration, Namespace, Secret }

object openApi {
  import com.virtuslab.iat.json.json4s.JValueTransformable
  import com.virtuslab.iat.json.playjson.PlayJsonTransformable
  import com.virtuslab.kubernetes.client.openapi.model

  object json4s extends JValueTransformable {
    import org.json4s.JValue
    object Interpreter extends InterpreterDerivation[Namespace, JValue]
  }
  object playjason extends PlayJsonTransformable {
    import play.api.libs.json.JsValue
    object Interpreter extends InterpreterDerivation[Namespace, JsValue]
  }

  // TODO circe

  implicit def namespaceInterpreter[R](
      implicit
      t: Transformer[model.Namespace, R]
    ): Interpreter[Namespace, Namespace, R] =
    (obj: Namespace, unused: Namespace) => {
      Support(
        model.Namespace(
          metadata = Some(model.ObjectMeta(name = Some(obj.name)))
        )
      ) :: Nil
    }

  implicit def configurationInterpreter[R](
      implicit
      t: Transformer[model.ConfigMap, R]
    ): Interpreter[Configuration, Namespace, R] = (obj: Configuration, ns: Namespace) => {
    Support(
      model.ConfigMap(
        metadata = Some(
          model.ObjectMeta(name = Some(obj.name), namespace = Some(ns.name))
        ),
        data = Some(obj.data)
      )
    ) :: Nil
  }

  implicit def secretInterpreter[R](implicit t: Transformer[model.Secret, R]): Interpreter[Secret, Namespace, R] =
    (obj: Secret, ns: Namespace) => {
      Support(
        model.Secret(
          metadata = Some(
            model.ObjectMeta(name = Some(obj.name), namespace = Some(ns.name))
          ),
          data = Some(obj.data.view.mapValues(_.getBytes).toMap)
        )
      ) :: Nil
    }

  implicit def applicationInterpreter[R](
      implicit
      t1: Transformer[model.Service, R],
      t2: Transformer[model.Deployment, R]
    ): Interpreter[Application, Namespace, R] = (obj: Application, ns: Namespace) => {
    val meta = model.ObjectMeta(name = Some(obj.name), namespace = Some(ns.name))

    val service = model.Service(
      metadata = Some(meta),
      spec = Some(model.ServiceSpec())
    )

    val deployment = model.Deployment(
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

    Support(service) :: Support(deployment) :: Nil
  }
}
