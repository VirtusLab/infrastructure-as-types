package com.virtuslab.iat.skuber

import _root_.skuber.apps.v1.Deployment
import _root_.skuber.{ ConfigMap, Service, Namespace => SNamespace }
import com.virtuslab.iat.dsl.Label
import com.virtuslab.iat.kubernetes.dsl.{ Application, Configuration, Namespace }

trait DefaultDeinterpreters {
  import Label.ops._

  type Maybe[B] = Either[Throwable, B]

  implicit val namespaceDeinterpreter: Maybe[SNamespace] => Maybe[Namespace] =
    (m: Maybe[SNamespace]) =>
      m.map(
        ns =>
          Namespace(
            labels = ns.metadata.labels.map(fromMap).toList
          )
      )

  implicit val applicationDeinterpreter: (Maybe[Service], Maybe[Deployment]) => Maybe[Application] =
    (svc: Maybe[Service], dpl: Maybe[Deployment]) =>
      (svc, dpl) match {
        case (Right(svc), Right(dpl)) =>
          Right(
            Application(
              labels = (svc.metadata.labels.map(fromMap) ++ dpl.metadata.labels.map(fromMap)).toSet.toList
              // TODO
            )
          )
        case (Right(_), Left(e)) => Left(e)
        case (Left(e), _)        => Left(e)
      }

  implicit val configurationDeinterpreter: Maybe[ConfigMap] => Maybe[Configuration] = {
    case Right(conf) =>
      Right(
        Configuration(
          labels = conf.metadata.labels.map(fromMap).toList,
          data = conf.data
        )
      )
    case Left(e) => Left(e)
  }

  implicit class NamespaceDeinterpreterOps(obj: Maybe[SNamespace]) {
    def deinterpret(
        implicit
        d: Maybe[SNamespace] => Maybe[Namespace]
      ): Maybe[Namespace] = d(obj)
  }

  implicit class ApplicationDeinterpreterOps(obj: Product2[Maybe[Service], Maybe[Deployment]]) {
    def deinterpret(
        implicit
        d: (Maybe[Service], Maybe[Deployment]) => Maybe[Application]
      ): Maybe[Application] = d(obj._1, obj._2)
  }

  implicit class ConfigurationDeinterpreter(obj: Maybe[ConfigMap]) {
    def deinterpret(
        implicit
        d: Maybe[ConfigMap] => Maybe[Configuration]
      ): Maybe[Configuration] = d(obj)
  }
}
