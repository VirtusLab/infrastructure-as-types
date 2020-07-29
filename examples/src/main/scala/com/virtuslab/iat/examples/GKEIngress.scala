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
    Name("gke-example") :: Nil,
    inputs = Protocols(
      Protocol.Layers(l7 = HTTP(), l4 = TCP())
    ),
    outputs = Protocols(
      Protocol.Layers(l7 = HTTP(path = HTTP.Path("/"), host = HTTP.Host(app1.name)), l4 = TCP(Port(app1Port))),
      Protocol.Layers(l7 = HTTP(path = HTTP.Path("/kube"), host = HTTP.Host(app2.name)), l4 = TCP(Port(app2Port)))
    )
  )

  import iat.skuber.details._
  import com.virtuslab.iat.examples.details.nativeService

  val app1Details = (
    nativeService,
    replicas(2)
  )
  val app2Details = (
    nativeService,
    replicas(2)
  )

  val managedCert = custom.ManagedCertificate(
    name = "gke-example",
    spec = custom.ManagedCertificate.Spec(
      domains = "gke-example.bluecatcode.com" :: Nil
    )
  )

  val ingressDetails = details.managedTLS(
    addressName = "gke-example",
    certificateName = managedCert.name
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
      gw.inNamespace(gke).interpret.map(ingressDetails).upsert.deinterpret.summary :: Nil

  results.foreach(s => println(s.asString))

  val mngcrt = managedCert.map(_.withNamespace(gke.name)).upsert
  println("mngcrt:" + mngcrt)

  // Cleanup
  close()
}

object details {

  import skuber.ext.Ingress
  import skuber.Service
  import com.softwaremill.quicklens._
  import iat.skuber.details._

  def nativeService: Service => Service = {
    val annotations = Map(
      "cloud.google.com/neg" -> "{\"ingress\": true}"
    )
    val annotate = (s: Service) => s.modify(_.metadata.annotations).using(_ ++ annotations)
    annotate.andThen(serviceType(Service.Type.ClusterIP))
  }

  def managedTLS(addressName: String, certificateName: String): Ingress => Ingress = {
    val annotations = Map(
      "kubernetes.io/ingress.global-static-ip-name" -> addressName,
      "networking.gke.io/managed-certificates" -> certificateName
    )
    (i: Ingress) => i.modify(_.metadata.annotations).using(_ ++ annotations)
  }
}

object custom {
  import skuber.CustomResource
  import skuber.ListResource
  import skuber.ResourceDefinition
  import skuber.ResourceSpecification.Subresources
  import skuber.apiextensions.CustomResourceDefinition
  import play.api.libs.json.{ Format, Json }

  type ManagedCertificate = CustomResource[ManagedCertificate.Spec, ManagedCertificate.Status]
  type ManagedCertificateList = ListResource[ManagedCertificate]

  //noinspection TypeAnnotation
  object ManagedCertificate {
    case class Spec(domains: List[String])
    case class Status(
        certificateName: Option[String] = None,
        certificateStatus: Option[String] = None,
        expireTime: Option[skuber.Timestamp] = None,
        domainStatus: List[Status.DomainStatus] = Nil)
    object Status {
      case class DomainStatus(domain: Option[String] = None, status: Option[String] = None)
      implicit val domainStatusFmt: Format[DomainStatus] = Json.format[DomainStatus]
    }

    implicit val specFmt: Format[Spec] = Json.format[Spec]
    implicit val statusFmt: Format[Status] = Json.format[Status]

    implicit val managedCertificateResourceDefinition = ResourceDefinition[ManagedCertificate](
      group = "networking.gke.io",
      version = "v1beta2",
      kind = "ManagedCertificate",
      subresources = Some(Subresources().withStatusSubresource)
    )

    implicit val statusSubEnabled = CustomResource.statusMethodsEnabler[ManagedCertificate]

    val crd = CustomResourceDefinition[ManagedCertificate]

    def apply(name: String, spec: Spec) =
      CustomResource[Spec, Status](spec)(managedCertificateResourceDefinition).withName(name)
  }
}
