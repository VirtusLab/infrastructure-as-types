package com.virtuslab.iat.kubernetes.interpreter.skuber

import _root_.skuber.apps.v1.Deployment
import _root_.skuber.{ Service, Namespace => SNamespace }
import com.virtuslab.iat.core
import com.virtuslab.iat.dsl.Label
import com.virtuslab.iat.dsl.kubernetes.{ Application, Namespace }
import com.virtuslab.iat.kubernetes.skuber.Base

trait BaseDeinterpreters {
  import Label.ops._

  type Error
  type Result[B <: Any] <: core.Result[B]
  type MaybeResult[B <: Base] <: Result[Either[Error, B]]

  implicit val namespaceDeinterpreter: MaybeResult[SNamespace] => Either[Error, Namespace] =
    (m: MaybeResult[SNamespace]) =>
      m.result.map(
        ns =>
          Namespace(
            labels = ns.metadata.labels.map(fromMap).toList
          )
      )

  implicit val applicationDeinterpreter: (MaybeResult[Service], MaybeResult[Deployment]) => Either[Error, Application] =
    (svc: MaybeResult[Service], dpl: MaybeResult[Deployment]) =>
      (svc.result, dpl.result) match {
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
}
