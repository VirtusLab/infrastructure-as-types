package com.virtuslab.iat.examples

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.virtuslab.iat.dsl.Labeled
import com.virtuslab.iat.scala.unit.ops._
import play.api.libs.json._
import skuber.api.Configuration
import skuber.api.client.{ Context, LoggingContext }

import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContextExecutor }

abstract class SkuberApp {

  import skuber._

  implicit private val system: ActorSystem = ActorSystem("a-system")
  implicit protected val actorMaterializer: ActorMaterializer = ActorMaterializer()
  implicit protected val dispatcher: ExecutionContextExecutor = system.dispatcher

  private val kubeconfig: Configuration = api.Configuration.parseKubeconfigFile().get
  private val defaultContextNameEnvVar = "IAT_KUBE_CONTEXT"
  private val ourContext: Context = Option(System.getenv(defaultContextNameEnvVar)) match {
    case Some(contextName) =>
      kubeconfig.contexts.getOrElse(
        contextName,
        default = throw new IllegalStateException(s"No '$contextName' Kubernetes context present in ~/.kube/config")
      )
    case None =>
      val defaultContextName = "gke_infrastructure-as-types_us-central1-a_standard-cluster-1"
      kubeconfig.contexts.getOrElse(
        defaultContextName,
        default = throw new IllegalStateException(
          s"'$defaultContextNameEnvVar' env var undefined and no '$defaultContextName' Kubernetes context present in ~/.kube/config"
        )
      )
  }
  private val configWithContext = kubeconfig.useContext(ourContext)

  implicit protected val client: K8SRequestContext = k8sInit(
    config = configWithContext,
    appConfig = system.settings.config
  )
  implicit protected val lc: LoggingContext = LoggingContext.lc

  def close(): Unit = {
    println("All done.")
    client.close // AFAIU it does nothing
    Await.result(Http().shutdownAllConnectionPools(), 1.minute) // This actually closes the Akka HTTP client (Skuber) connection pool
    Await.result(system.terminate(), 1.minute).toUnit()
    actorMaterializer.shutdown()
  }

  case class Summary(msg: String) {
    def asString: String = msg
  }

  import scala.reflect.runtime.{ universe => ru }
  object Summary {
    import com.virtuslab.iat.skuber.playjson.asYamlString
    import play.api.libs.functional.syntax._
    import skuber.api.client._
    import skuber.json.format._

    val anyWrites: Writes[Any] = (any: Any) => anyToJsValue(any)

    // this handler writes a generic Status response from the server
    // https://github.com/kubernetes/apimachinery/blob/kubernetes-1.18.2/pkg/apis/meta/v1/types.go#L599
    implicit val statusWrites: Writes[Status] = (
      (JsPath \ "apiVersion").write[String] and
        (JsPath \ "kind").write[String] and
        (JsPath \ "metadata").write[ListMeta] and
        (JsPath \ "status").writeNullable[String] and
        (JsPath \ "message").writeNullable[String] and
        (JsPath \ "reason").writeNullable[String] and
        (JsPath \ "details").writeNullable[Any](anyWrites) and /* why would anyone use Any?! */
      (JsPath \ "code").writeNullable[Int]
    )(unlift(Status.unapply))

    @SuppressWarnings(Array("org.wartremover.warts.Recursion", "org.wartremover.warts.ToString"))
    def anyToJsValue(m: Any): JsValue = {
      m match {
        case s: String     => JsString(s)
        case n: Int        => JsNumber(n)
        case n: Long       => JsNumber(n)
        case n: Double     => JsNumber(n)
        case n: BigDecimal => JsNumber(n)
        case b: Boolean    => JsBoolean(b)
        case l: Seq[_]     => JsArray(l.map(anyToJsValue))
        case o: Map[_, _]  => JsObject(o.map { case (k, v) => k.toString -> anyToJsValue(v) })
        case o: JsObject   => o
      }
    }

    //noinspection ScalaUnusedSymbol
    def getTypeTag[T: ru.TypeTag](unused: T): ru.TypeTag[T] = ru.typeTag[T]

    def apply[A: ru.TypeTag](result: Either[Throwable, A]): Summary = result match {
      case Left(err) =>
        (err, ru.typeTag[A].tpe) match {
          case (err: K8SException, name) => Summary(s"""|Error[${err.status.code.getOrElse("unknown")}]: '$name', details:
                |${asYamlString(err.status)}""".stripMargin)
          case (err, name)               => Summary(s"Error: '$name', details: $err")
        }
      case Right(result) =>
        (result, getTypeTag(result).tpe) match {
          case (o: Labeled, name) =>
            Summary(
              s"OK: '$name' labeled: '${o.labels.map(l => s"${l.key}: ${l.value}").mkString(", ")}'"
            )
          case (o: ObjectResource, name) =>
            Summary(
              s"OK: '$name' labeled: '${o.metadata.labels.map(t => s"${t._1}: ${t._2}").mkString(", ")}'"
            )
          case (o, name) => Summary(s"OK: $name, $o")
        }
    }
  }

  implicit class SummaryOps1[A: ru.TypeTag](r1: Either[Throwable, A]) {
    def summary(): Summary = Summary(r1)
  }

  implicit class SummaryOps2[A1: ru.TypeTag, A2: ru.TypeTag](r: (Either[Throwable, A1], Either[Throwable, A2])) {
    def summary(): List[Summary] = Summary(r._1) :: Summary(r._2) :: Nil
  }
}
