package com.virtuslab

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import skuber.api.Configuration
import skuber.api.client.Context

import scala.concurrent.ExecutionContextExecutor
import scala.util.{ Failure, Success }

abstract class DSLMain {

  import skuber._
  import skuber.json.format._

  implicit private val system: ActorSystem = ActorSystem("")
  implicit private val actorMaterializer: ActorMaterializer =
    ActorMaterializer()
  implicit private val dispatcher: ExecutionContextExecutor = system.dispatcher

  private val kubeconfig: Configuration =
    api.Configuration.parseKubeconfigFile().get
  private val ourContext: Context = kubeconfig.contexts(
    "gke_infrastructure-as-types_us-central1-a_standard-cluster-1"
  )
  private val configWithContext = kubeconfig.useContext(ourContext)

  private val client: K8SRequestContext =
    k8sInit(config = configWithContext, appConfig = system.settings.config)

  private val namespace = Namespace.forName("test")

  private val create = for {
    ns <- client.create(namespace)
  } yield ns

  create.onComplete {
    case Success(_) =>
      println("Successfully created resources on Kubernetes cluster")
    case Failure(ex) =>
      throw new Exception(
        "Encountered exception trying to create resources on Kubernetes cluster: ",
        ex
      )
  }
}
