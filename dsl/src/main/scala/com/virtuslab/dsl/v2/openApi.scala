package com.virtuslab.dsl.v2

object openApi {
  import com.virtuslab.kubernetes.client.openapi.model

  object interpreters {

    implicit def namespaceInterpreter[R](
        implicit
        t: model.Namespace => Transformable[model.Namespace, R]
      ): Interpreter[Namespace, R] =
      new Interpreter[Namespace, R] {
        override def interpret(obj: Namespace, ns: Namespace): List[Support[_, R]] = {
          Support(
            model.Namespace(
              metadata = Some(model.ObjectMeta(name = Some(obj.name)))
            )
          ) :: Nil
        }
      }

    implicit def configurationInterpreter[R](
        implicit
        t: model.ConfigMap => Transformable[model.ConfigMap, R]
      ): Interpreter[Configuration, R] = new Interpreter[Configuration, R] {
      override def interpret(obj: Configuration, ns: Namespace): List[Support[_, R]] = {
        Support(
          model.ConfigMap(
            metadata = Some(
              model.ObjectMeta(name = Some(obj.name), namespace = Some(ns.name))
            ),
            data = Some(obj.data)
          )
        ) :: Nil
      }
    }

    implicit def secretInterpreter[R](implicit t: model.Secret => Transformable[model.Secret, R]): Interpreter[Secret, R] =
      new Interpreter[Secret, R] {
        override def interpret(obj: Secret, ns: Namespace): List[Support[_, R]] = {
          Support(
            model.Secret(
              metadata = Some(
                model.ObjectMeta(name = Some(obj.name), namespace = Some(ns.name))
              ),
              data = Some(obj.data.view.mapValues(_.getBytes).toMap)
            )
          ) :: Nil
        }
      }

    implicit def applicationInterpreter[R](
        implicit
        t1: model.Service => Transformable[model.Service, R],
        t2: model.Deployment => Transformable[model.Deployment, R]
      ): Interpreter[Application, R] = new Interpreter[Application, R] {
      override def interpret(obj: Application, ns: Namespace): List[Support[_, R]] = {
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
}
