package com.virtuslab.iat.examples

import com.virtuslab.iat
import com.virtuslab.iat.dsl.Label.Name
import com.virtuslab.iat.dsl.{ HTTP, Port, Protocol, Protocols, TCP }
import com.virtuslab.iat.kubernetes.dsl.{ Application, Container, Gateway, Namespace }

object GKEIngress extends SkuberApp with App {

  val gke = Namespace(Name("gke") :: Nil)

  val app1Port = 60000
  val app1 = Application(
    Name("hello-world") :: Nil,
    Container(
      Name("hello") :: Nil,
      image = "gcr.io/google-samples/hello-app:2.0",
      envs = "PORT" -> s"$app1Port" :: Nil,
      ports = TCP(app1Port) :: Nil
    ) :: Nil
  )

  val app2Port = 8080
  val app2 = Application(
    Name("hello-kubernetes") :: Nil,
    Container(
      Name("hello-again") :: Nil,
      image = "gcr.io/google-samples/node-hello:1.0",
      envs = "PORT" -> s"$app2Port" :: Nil,
      ports = TCP(app2Port) :: Nil
    ) :: Nil
  )

  val gw = Gateway(
    Name("my-ingress") :: Nil,
    inputs = Protocols(
      Protocol.Layers(l7 = HTTP(), l4 = TCP())
    ),
    outputs = Protocols(
      Protocol.Layers(l7 = HTTP(path = HTTP.Path("/"), host = HTTP.Host(app1.name)), l4 = TCP(Port(app1Port))),
      Protocol.Layers(l7 = HTTP(path = HTTP.Path("/kube"), host = HTTP.Host(app2.name)), l4 = TCP(Port(app2Port)))
    )
  )

  import iat.skuber.details._
  import skuber.Service
  def nativeService: Service => Service = {
    import com.softwaremill.quicklens._
    val annotate = (s: skuber.Service) =>
      s.modify(_.metadata.annotations)
        .using(
          _ ++ Map("cloud.google.com/neg" -> "{\"ingress\": true}")
        )
    annotate.andThen(serviceType(Service.Type.ClusterIP))
  }

  val app1Details = (
    nativeService,
    replicas(2)
  )
  val app2Details = (
    nativeService,
    replicas(2)
  )

  import iat.kubernetes.dsl.experimental._
  import iat.skuber.deployment._
  import iat.skuber.experimental._
  import skuber.json.ext.format._
  import skuber.json.format._

  val results =
    gke.interpret.upsert.deinterpret.summary ::
      app1.inNamespace(gke).interpret.map(app1Details).upsert.deinterpret.summary ::
      app2.inNamespace(gke).interpret.map(app2Details).upsert.deinterpret.summary ::
      gw.inNamespace(gke).interpret.upsert.deinterpret.summary :: Nil

  results.foreach(s => println(s.asString))

  // Cleanup
  close()
}
