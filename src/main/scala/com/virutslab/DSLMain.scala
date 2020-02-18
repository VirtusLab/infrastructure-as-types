package com.virutslab

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import skuber.api.Configuration
import skuber.api.client.Context

abstract class DSLMain {

  import skuber._
  import skuber.json.format._

  implicit private val system: ActorSystem = ActorSystem("")
  implicit private val actorMaterializer: ActorMaterializer = ActorMaterializer()

  private val kubeconfig: Configuration = api.Configuration.parseKubeconfigFile().get
  private val ourContext: Context = kubeconfig.contexts("gke_infrastructure-as-types_us-central1-a_standard-cluster-1")
  private val configWithContext = kubeconfig.useContext(ourContext)

  private val client: K8SRequestContext = k8sInit(config = configWithContext, appConfig = system.settings.config)
}
