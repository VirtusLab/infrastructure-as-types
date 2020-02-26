package com.virtuslab

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.{ Failure, Success }

object TestMain extends DSLMain with App {

  import skuber._
  import skuber.json.format._

  private val namespace = Namespace.forName("test")

  // Ensure a namespace
  private val create = for {
    ns <- client.create(namespace) recover {
      case ex: K8SException if ex.status.code.contains(409) => client.update(namespace) // this need an abstraction
    }
  } yield ns

  Await.result(create, 1.minute) match {
    case Success(_)  => println("Successfully created resources on Kubernetes cluster")
    case Failure(ex) => throw new Exception("Encountered exception trying to create resources on Kubernetes cluster: ", ex)
  }

  // Populate the namespace
  private val nsClient = client.usingNamespace(namespace.name)

  // Cleanup
  client.close
  system.terminate().foreach { f =>
    System.exit(0)
  }
}
