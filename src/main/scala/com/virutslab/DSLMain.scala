package com.virutslab

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import skuber.api.Configuration

abstract class DSLMain {

  import skuber._
  import skuber.json.format._

  implicit private val system: ActorSystem = ActorSystem("")
  implicit private val actorMaterializer: ActorMaterializer = ActorMaterializer()

  private val config: Configuration = Configuration()

  private val client: K8SRequestContext = k8sInit(config = config, appConfig = system.settings.config)

}
