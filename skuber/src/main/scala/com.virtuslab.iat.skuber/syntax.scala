package com.virtuslab.iat.skuber

import com.virtuslab.iat.scala.{ FunctionOps, TupleOps }

object dsl {
  import com.virtuslab.iat.kubernetes.dsl.Mountable.MountSource
  import com.virtuslab.iat.kubernetes.dsl.{ Configuration, Secret }
  import skuber.Volume.{ ConfigMapVolumeSource, Secret => SecretVolumeSource }

  implicit val secretMountSource: MountSource[Secret, SecretVolumeSource] =
    (obj: Secret) => SecretVolumeSource(secretName = obj.name)

  implicit val configurationMountSource: MountSource[Configuration, ConfigMapVolumeSource] =
    (obj: Configuration) => ConfigMapVolumeSource(name = obj.name)
}

object deployment extends ApiOps with DefaultInterpreters with DefaultDeinterpreters with InterpreterOps with TupleOps

object playjson extends PlayJsonProcessors with DefaultInterpreters with InterpreterOps with TupleOps

object interpreter extends DefaultInterpreters
object subinterpreter extends DefaultSubinterpreters
object details extends DefaultDetails with FunctionOps
object deinterpreter extends DefaultDeinterpreters

object experimental {
  import akka.stream.Materializer
  import com.virtuslab.iat.core.experimental.Evolution
  import com.virtuslab.iat.core.experimental.Evolution.Pair
  import com.virtuslab.iat.core.experimental.ImplicitInterpretation
  import com.virtuslab.iat.dsl.Interpretable
  import com.virtuslab.iat.kubernetes.dsl.{ Application, Configuration, Namespace }
  import skuber.Service
  import skuber.apps.v1.Deployment
  import play.api.libs.json.Format
  import skuber.{ K8SRequestContext, ObjectResource, ResourceDefinition }
  import skuber.api.client.LoggingContext
  import scala.concurrent.duration._
  import scala.concurrent.{ Await, ExecutionContext }
  import scala.util.Try

  // Adds interpretedImplicitly method for Skuber interpretations
  implicit class SkuberInterpretationOps[A <: Interpretable[A]](val arguments: A)
    extends ImplicitInterpretation[A, ObjectResource]

  // ensure D1
  // ensure S1
  // create D2
  // point S1 to D1 and D2
  // wait for ready
  // point S1 to D2 only
  // delete D1
  // or rollback and return error at any point
  implicit val blueGreenApplication: BlueGreenApplicationStrategy = new BlueGreenApplicationStrategy

  class BlueGreenApplicationStrategy
    extends Evolution.Strategy[(Application, Namespace), (Service, Deployment), (Application, Namespace), (Service, Deployment)] {
    def execute(
        pair: Pair[(Application, Namespace), (Service, Deployment), (Application, Namespace), (Service, Deployment)]
      )(implicit
        executor: ExecutionContext,
        client: K8SRequestContext,
        lc: LoggingContext,
        materializer: Materializer,
        svcFormat: Format[Service],
        svcResDef: ResourceDefinition[Service],
        dplFormat: Format[Deployment],
        dplResDef: ResourceDefinition[Deployment]
      ): Either[Throwable, ((Application, Namespace), (Service, Deployment))] =
      Try {
        val api = SimpleDeployer
        val (currentArguments, currentInterpreter, currentDetails) = pair.current
        val (s1, d1) = currentInterpreter.andThen(currentDetails)(currentArguments)
        val (targetArguments, targetInterpreter, targetDetails) = pair.target
        val (s2, d2) = targetInterpreter.andThen(targetDetails)(targetArguments)

        // version would probably be cleaner, but do we want to require that?
        def targetSuffix(name: String) = name match {
          case n if n.endsWith("blue")  => "green"
          case n if n.endsWith("green") => "blue"
          case _                        => "green"
        }
        println("......blue/green start")

        api.upsert(d1).toTry.get
        println("......d1 ensured")
        api.upsert(s1).toTry.get
        println("......s1 ensured")

        // label "name" stays the same, only the "metadata.name" changes to avoid conflict
        val renamedD2 = d2.copy(metadata = d2.metadata.copy(name = targetSuffix(d2.name)))
        val d2r = api.upsert(renamedD2).toTry.get
        println("......d2r ensured")

        // wait for ready
        val futureReady = api.readinessWatch(d2r) { event =>
          {
            val available = event._object.status
              .map(
                _.conditions
                  .filter(
                    _.reason.contains("MinimumReplicasAvailable")
                  )
                  .filter(
                    _.status == "True"
                  )
              )
              .fold(false)(_.nonEmpty)
            println(s".........($available): " + event._object.status)
            available
          }
        }
        Await.result(futureReady, 10.minute)
        println("......d2r ready")

        val s2r = api.upsert(s2).toTry.get
        println("......s2r done")

        api.delete(d1).toTry.get
        println("......d1 deleted")
        (targetArguments, (s2r, d2r))
      }.toEither
  }

  implicit val upsertNamespace: UpsertStrategy[Namespace, skuber.Namespace] =
    new UpsertStrategy[Namespace, skuber.Namespace]
  implicit val upsertConfiguration: UpsertStrategy[(Configuration, Namespace), skuber.ConfigMap] =
    new UpsertStrategy[(Configuration, Namespace), skuber.ConfigMap]
  implicit val upsertApplication: UpsertStrategy2[(Application, Namespace), Service, Deployment] =
    new UpsertStrategy2[(Application, Namespace), Service, Deployment]

  class UpsertStrategy[A, B <: ObjectResource] {
    def execute(
        o: (A, A => B, B => B)
      )(implicit
        format: Format[B],
        resDef: ResourceDefinition[B],
        executor: ExecutionContext,
        client: K8SRequestContext,
        lc: LoggingContext
      ): Either[Throwable, B] =
      Try {
        val (arguments, interpreter, details) = o
        val ns = interpreter.andThen(details)(arguments)

        val api = SimpleDeployer
        api.upsert(ns).toTry.get
      }.toEither
  }

  class UpsertStrategy2[A, B1 <: ObjectResource, B2 <: ObjectResource] {
    def execute(
        o: (A, A => (B1, B2), (B1, B2) => (B1, B2))
      )(implicit
        format1: Format[B1],
        format2: Format[B2],
        resDef1: ResourceDefinition[B1],
        resDef2: ResourceDefinition[B2],
        executor: ExecutionContext,
        client: K8SRequestContext,
        lc: LoggingContext
      ): Either[Throwable, (B1, B2)] =
      Try { // TODO can this be implemented by composing UpsertStrategy twice?
        val (arguments, interpreter, details) = o
        val (b1, b2) = interpreter.andThen(details.tupled)(arguments)

        val api = SimpleDeployer
        (api.upsert(b1).toTry.get, api.upsert(b2).toTry.get)
      }.toEither

    def execute[A1, A2](
        o: ((A1, A2), ((A1, A2)) => (B1, B2), ((B1, B2)) => (B1, B2))
      )(implicit
        evA: A =:= (A1, A2), // TODO could this be done by extending UpsertStrategy with various type params?
        format1: Format[B1],
        format2: Format[B2],
        resDef1: ResourceDefinition[B1],
        resDef2: ResourceDefinition[B2],
        executor: ExecutionContext,
        client: K8SRequestContext,
        lc: LoggingContext
      ): Either[Throwable, (B1, B2)] = {
      val (arguments, interpreter, details) = o
      execute(
        arguments.asInstanceOf[A],
        interpreter.asInstanceOf[A => (B1, B2)],
        details(_, _)
      )
    }
  }
}
