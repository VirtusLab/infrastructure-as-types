package com.virtuslab.iat.skuber

import akka.Done
import akka.http.scaladsl.model.StatusCodes
import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import play.api.libs.json.Format
import skuber.api.client.LoggingContext
import skuber.{ K8SException, K8SRequestContext, K8SWatchEvent, ObjectMeta, ObjectResource, ResourceDefinition, Service }

import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.util.Try

trait SimpleDeployer {
  protected def futureGet[A <: skuber.ObjectResource: Format: ResourceDefinition](
      o: A
    )(implicit
      executor: ExecutionContext,
      client: K8SRequestContext,
      lc: LoggingContext
    ): Future[Option[A]] = {
    client.getInNamespace(o.name, o.namespace) map { result =>
      Some(result)
    } recover {
      case ex: K8SException if ex.status.code.contains(StatusCodes.NotFound.intValue) => None
    }
  }

  protected def futureUpdate[A <: ObjectResource: Format: ResourceDefinition](
      o: A
    )(implicit
      executor: ExecutionContext,
      client: K8SRequestContext,
      lc: LoggingContext
    ): Future[A] = o match {
    case s: Service => // FIXME can we do the upsert safer and more general with e.g. with a deep copy?
      val future = futureGet(o)
      val maybeService = Await.result(future, 1.minute)
      if (maybeService.isDefined) {
        def copyClusterIpIfExists(b: Service)(a: Service.Spec): Service.Spec =
          a.copy(clusterIP = b.spec.map(_.clusterIP).getOrElse(a.clusterIP))
        def patchWithOriginal(s: Service, orig: Service): Service = s.copy(
          metadata = s.metadata.copy(resourceVersion = orig.metadata.resourceVersion),
          spec = s.spec.map(copyClusterIpIfExists(orig))
        )

        val svc = maybeService.get.asInstanceOf[Service]
        println(s"Patching '$s' on Kubernetes cluster's original: '$svc'")
        val patched = patchWithOriginal(s, svc).asInstanceOf[A]
        client.usingNamespace(o.namespace).update(patched)
      } else {
        println(s"Not found '$s' on Kubernetes cluster, trying to update anyway")
        client.usingNamespace(o.namespace).update(o)
      }
    case o => client.usingNamespace(o.namespace).update(o)
  }

  protected def futureUpsert[A <: ObjectResource: Format: ResourceDefinition](
      o: A
    )(implicit
      executor: ExecutionContext,
      client: K8SRequestContext,
      lc: LoggingContext
    ): Future[A] = {
    futureCreate(o) recoverWith {
      case ex: K8SException if ex.status.code.contains(StatusCodes.Conflict.intValue) => futureUpdate(o)
    }
  }

  protected def futureCreate[A <: ObjectResource: Format: ResourceDefinition](
      o: A
    )(implicit
      executor: ExecutionContext,
      client: K8SRequestContext,
      lc: LoggingContext
    ): Future[A] = client.usingNamespace(o.namespace).create(o)

  protected def futureDelete[A <: ObjectResource: Format: ResourceDefinition](
      o: A
    )(implicit
      executor: ExecutionContext,
      client: K8SRequestContext,
      lc: LoggingContext
    ): Future[Unit] = client.usingNamespace(o.namespace).delete(o.name)

}

object SimpleDeployer extends SimpleDeployer {
  def upsert[A <: ObjectResource: Format: ResourceDefinition](
      o: A
    )(implicit
      executor: ExecutionContext,
      client: K8SRequestContext,
      lc: LoggingContext
    ): Either[Throwable, A] =
    Try {
      val future = futureUpsert(o)
      val result = Await.result(future, 1.minute)
      println(s"Successfully created or updated '$result' on Kubernetes cluster")
      result
    }.toEither

  def create[A <: ObjectResource: Format: ResourceDefinition](
      o: A
    )(implicit
      executor: ExecutionContext,
      client: K8SRequestContext,
      lc: LoggingContext
    ): Either[Throwable, A] =
    Try {
      val future = futureCreate(o)
      val result = Await.result(future, 1.minute)
      println(s"Successfully created '$result' on Kubernetes cluster")
      result
    }.toEither

  def delete[A <: ObjectResource: Format: ResourceDefinition](
      o: A
    )(implicit
      executor: ExecutionContext,
      client: K8SRequestContext,
      lc: LoggingContext
    ): Either[Throwable, Unit] =
    Try {
      val future = futureDelete(o)
      Await.result(future, 1.minute)
      println(s"Successfully deleted '$o' from Kubernetes cluster")
    }.toEither

  def readinessWatch[A <: ObjectResource: Format: ResourceDefinition](
      o: A
    )(
      f: K8SWatchEvent[A] => Boolean
    )(implicit
      executor: ExecutionContext,
      client: K8SRequestContext,
      lc: LoggingContext,
      materializer: Materializer
    ): Future[Done] = {
    client
      .usingNamespace(o.namespace)
      .watchContinuously(o)
      .takeWhile(f.andThen(!_))
      .runWith(Sink.foreach { o =>
        println("........." + o)
      }) // TODO do we need the actual object returned?
  }
}
