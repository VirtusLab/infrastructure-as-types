package com.virtuslab.dsl.v2

object openApi {
  import com.virtuslab.kubernetes.client.openapi.model

  object implementation {
    object json4s {
      import org.json4s.JValue
      object Transformer extends JValueTransformable
      object Interpreter extends InterpreterDerivation[JValue]
    }
    object playjason {
      import play.api.libs.json.JsValue
      object Transformer extends PlayJsonTransformable
      object Interpreter extends InterpreterDerivation[JsValue]
    }
    // TODO circe
  }

  object interpreters {

    implicit def namespaceInterpreter[R](
        implicit
        t: model.Namespace => Transformable[model.Namespace, R]
      ): Interpreter[Namespace, R] =
      (obj: Namespace, ns: Namespace) => {
        Support(
          model.Namespace(
            metadata = Some(model.ObjectMeta(name = Some(obj.name)))
          )
        ) :: Nil
      }

    implicit def configurationInterpreter[R](
        implicit
        t: model.ConfigMap => Transformable[model.ConfigMap, R]
      ): Interpreter[Configuration, R] = (obj: Configuration, ns: Namespace) => {
      Support(
        model.ConfigMap(
          metadata = Some(
            model.ObjectMeta(name = Some(obj.name), namespace = Some(ns.name))
          ),
          data = Some(obj.data)
        )
      ) :: Nil
    }

    implicit def secretInterpreter[R](implicit t: model.Secret => Transformable[model.Secret, R]): Interpreter[Secret, R] =
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
        t1: model.Service => Transformable[model.Service, R],
        t2: model.Deployment => Transformable[model.Deployment, R]
      ): Interpreter[Application, R] = (obj: Application, ns: Namespace) => {
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
}
