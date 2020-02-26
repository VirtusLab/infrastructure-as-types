package com.virtuslab

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import skuber.api.Configuration
import skuber.api.client.Context

import scala.concurrent.ExecutionContextExecutor

abstract class DSLMain {

  import skuber._

  implicit protected val system: ActorSystem = ActorSystem("")
  implicit private val actorMaterializer: ActorMaterializer = ActorMaterializer()
  implicit protected val dispatcher: ExecutionContextExecutor = system.dispatcher

  private val kubeconfig: Configuration = api.Configuration.parseKubeconfigFile().get
  private val ourContext: Context = kubeconfig.contexts("gke_infrastructure-as-types_us-central1-a_standard-cluster-1")
  private val configWithContext = kubeconfig.useContext(ourContext)

  protected val client: K8SRequestContext = k8sInit(config = configWithContext, appConfig = system.settings.config)
}
