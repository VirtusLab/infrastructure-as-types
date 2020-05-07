package com.virtuslab.iat.kubernetes.skubertest

import com.stephenn.scalatest.playjson.JsonMatchers
import com.virtuslab.iat.dsl.Label.{ App, Name }
import com.virtuslab.iat.dsl.kubernetes.{ Application, Container, Namespace, SelectedIPs }
import com.virtuslab.iat.dsl.{ IP, Port }
import com.virtuslab.iat.json.json4s.jackson.YamlMethods.yamlToJson
import com.virtuslab.iat.kubernetes.Metadata
import com.virtuslab.iat.test.EnsureMatchers
import com.virtuslab.iat.{ dsl, kubernetes }
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.JsValue

object InfractructureExample {
  import kubernetes.skuber.playjson._
  import skuber.json.format._

  val namespace = Namespace(Name("reactive-system") :: Nil)

  val api = Application(
    Name("api") :: Nil,
    Container(
      Name("api") :: Nil,
      image = "some.ecr.io/api:1.0",
      ports = Port(80) :: Nil
    ) :: Nil
  )

  val processor = Application(
    Name("processor") :: Nil,
    Container(
      Name("api") :: Nil,
      image = "some.ecr.io/api:1.0"
    ) :: Nil
  )

  val view = Application(
    Name("view") :: Nil,
    Container(
      Name("api") :: Nil,
      image = "some.ecr.io/api:1.0",
      ports = Port(8080) :: Nil
    ) :: Nil
  )

  val cassandra = Application(
    Name("cassandra-node") :: App("cassanrda") :: Nil,
    Container(
      Name("cassandra") :: Nil,
      image = "some.ecr.io/cassandra:3.0",
      ports = Port(6379) :: Nil //FIXME
    ) :: Nil
  )

  val postgres = Application(
    Name("postgres") :: App("postgres") :: Nil,
    Container(
      Name("postgres") :: Nil,
      image = "some.ecr.io/postgres:10.12",
      ports = Port(5432) :: Nil
    ) :: Nil
  )

  val kafka = Application(
    Name("kafka-node") :: App("kafka") :: Nil,
    Container(
      Name("master") :: Nil,
      image = "some.ecr.io/kafka:2.5.0",
      ports = Port(9092) :: Nil
    ) :: Nil
  )

  import dsl.kubernetes.Connection.ops._

  val connExtApi = api
    .communicatesWith(
      SelectedIPs(IP.Range("0.0.0.0/0")).ports(api.allPorts: _*)
    )
    .ingressOnly
    .named("external-api")

  val conApiKafka = api.communicatesWith(kafka).named("api-kafka")
  val conApiView = api.communicatesWith(view).egressOnly.named("api-kafka")
  val conViewPostgres = view.communicatesWith(postgres).egressOnly.named("view-postgres")
  val conKafkaProcessor = kafka.communicatesWith(processor).ingressOnly.named("kafka-processor")
  val conProcessorCassandra = processor.communicatesWith(cassandra).egressOnly.named("processor-cassandra")

  def resource: List[(Metadata, JsValue)] = {
    namespace.interpret.asMetaJsValues ++
      (api :: processor :: view :: cassandra :: postgres :: kafka :: Nil)
        .flatMap(_.interpret(namespace).reduce(_.asMetaJsValues)) ++
      (connExtApi :: conApiKafka :: conApiView :: conViewPostgres :: conKafkaProcessor :: conProcessorCassandra :: Nil)
        .flatMap(_.interpret(namespace).asMetaJsValues)
  }
}
