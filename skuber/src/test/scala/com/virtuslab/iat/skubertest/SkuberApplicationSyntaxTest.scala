package com.virtuslab.iat.skubertest

import com.virtuslab.iat
import com.virtuslab.iat.dsl.Label.{ Name, Role }
import com.virtuslab.iat.dsl.TCP
import com.virtuslab.iat.kubernetes.dsl.{ Application, Container, Namespace }
import com.virtuslab.iat.scalatest.EnsureMatchers
import com.virtuslab.iat.scalatest.playjson.JsonMatchers
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SkuberApplicationSyntaxTest extends AnyFlatSpec with Matchers with JsonMatchers with EnsureMatchers {

  it should "allow for intuitive syntax" in {
    val ns = Namespace(Name("foo") :: Nil)
    val app: Application = Application(
      Name("api") :: Nil,
      Container(
        Name("api") :: Nil,
        image = "some.ecr.io/api:1.0",
        ports = TCP(80) :: Nil
      ) :: Nil
    )

    import iat.kubernetes.dsl.experimental._
    import iat.skuber.details._
    import iat.skuber.experimental._
    import iat.skuber.interpreter._

    // (A, Namespace)
    // (A, Namespace) => (Service, Deployment)
    // (Service, Deployment) => (Service, Deployment)
    val v1App =
      app
        .inNamespace(ns)
        .interpretedImplicitly
        .withDetails(
          serviceUnchanged.merge(replicas(3))
        )

    val v2App =
      app
        .patch(a => a.copy(labels = Role("bar") :: a.labels))
        .inNamespace(ns)
        .interpretedWith(applicationInterpreter)
        .withDetails(
          serviceUnchanged.merge(replicas(2))
        )

//    val v1ToV2 = v1App.evolutionTo(v2App).withStrategy(blueGreenApplication)
    val v1ToV2 = v1App.evolutionTo(v2App)
//    blueGreenApplication.execute(v1ToV2)

    // TODO how to test this?
  }
}
