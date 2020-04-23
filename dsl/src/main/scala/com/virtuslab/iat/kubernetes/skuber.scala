package com.virtuslab.iat.kubernetes

import _root_.skuber.apps.v1.Deployment
import _root_.skuber.{ ConfigMap, LabelSelector, ObjectMeta, Pod, Service, Namespace => SNamespace, Secret => SSecret }
import com.virtuslab.iat.core.Transformable.Transformer
import com.virtuslab.iat.core.{ Interpreter, InterpreterDerivation, RootInterpreter, Support }
import com.virtuslab.iat.dsl._

object skuber {
  import com.virtuslab.iat.json.playjson.PlayJsonTransformable

  object playjson extends PlayJsonTransformable {
    import play.api.libs.json.JsValue

    object Interpreter extends InterpreterDerivation[Namespace, JsValue]
    // TODO object MetaExtractor extends JsValueMetadataExtractor
    // TODO object MetaExtractor extends ObjectResourceMetadataExtractor extends Transformer[ObjectResource, Metadata] ?
  }

  import Label.ops._
  import Secret.ops._

  def interpret[A, R](obj: A)(implicit i: RootInterpreter[A, R]): List[R] = Interpreter.interpret(obj)
  def interpret[A, C, R](obj: A, ctx: C)(implicit i: Interpreter[A, C, R]): List[R] = Interpreter.interpret(obj, ctx)

  implicit def namespaceInterpreter[R](
      implicit
      t: Transformer[SNamespace, R]
    ): RootInterpreter[Namespace, R] =
    (obj: Namespace) => {
      val namespace = SNamespace.from(
        ObjectMeta(
          name = obj.name,
          labels = obj.labels.asMap
        )
      )

      Support(namespace) :: Nil
    }

  implicit def configurationInterpreter[R](
      implicit
      t: Transformer[ConfigMap, R]
    ): Interpreter[Configuration, Namespace, R] =
    (obj: Configuration, ns: Namespace) => {
      val conf = ConfigMap(
        metadata = subinterpreter.objectMetaInterpreter(obj, ns),
        data = obj.data
      )

      Support(conf) :: Nil
    }

  implicit def secretInterpreter[R](
      implicit
      t: Transformer[SSecret, R]
    ): Interpreter[Secret, Namespace, R] =
    (obj: Secret, ns: Namespace) => {
      val secret = SSecret(
        metadata = subinterpreter.objectMetaInterpreter(obj, ns),
        data = obj.data.view.mapValues(_.getBytes).toMap
      )

      Support(secret) :: Nil
    }

  implicit def applicationInterpreter[R](
      implicit
      t1: Transformer[Service, R],
      t2: Transformer[Deployment, R]
    ): Interpreter[Application, Namespace, R] = (obj: Application, ns: Namespace) => {
    val meta = subinterpreter.objectMetaInterpreter(obj, ns)
    val service = subinterpreter.serviceInterpreter(meta)
    val deployment = subinterpreter.deploymentInterpreter(meta, obj)

    Support(service) :: Support(deployment) :: Nil
  }

  object subinterpreter {
    def objectMetaInterpreter(obj: Labeled with Named, ns: Namespace): ObjectMeta =
      ObjectMeta(
        name = obj.name,
        namespace = ns.name,
        labels = obj.labels.asMap
      )

    def serviceInterpreter(meta: ObjectMeta): Service = Service(
      metadata = meta,
      spec = Some(
        Service.Spec(
          // FIXME
        )
      )
    )

    def deploymentInterpreter(meta: ObjectMeta, obj: Application): Deployment = {
      val dplSpec = Deployment.Spec(
        selector = LabelSelector(
          obj.labels.map(l => LabelSelector.IsEqualRequirement(l.key, l.value)): _*
        ),
        template = Pod.Template
          .Spec(
            spec = Some(
              Pod.Spec(
                containers = Nil, // FIXME
                volumes = Nil // FIXME
              )
            )
          )
          .addLabels(
            obj.labels.asMap
          )
      )

      Deployment(
        metadata = meta,
        spec = Some(dplSpec)
      )
    }
  }
}
