package com.virtuslab.iat.examples

import _root_.skuber.Resource.Quantity
import _root_.skuber.{ Resource, Service }
import com.virtuslab.iat
import com.virtuslab.iat.dsl.Label.{ App, Name, Role, Tier }
import com.virtuslab.iat.dsl.{ IP, Peer, TCP }
import com.virtuslab.iat.kubernetes.dsl._

object GuestBook extends SkuberApp with scala.App {

  val guestbook = Namespace(Name("guestbook") :: Nil)

  val redisMaster = Application(
    Name("redis-master") :: App("redis") :: Role("master") :: Tier("backend") :: Nil,
    Container(
      Name("master") :: Nil,
      image = "k8s.gcr.io/redis:e2e",
      ports = TCP(6379) :: Nil
    ) :: Nil
  )

  val redisSlave = Application(
    Name("redis-slave") :: App("redis") :: Role("slave") :: Tier("backend") :: Nil,
    Container(
      Name("slave") :: Nil,
      image = "gcr.io/google_samples/gb-redisslave:v3",
      ports = TCP(6379) :: Nil,
      envs = "GET_HOSTS_FROM" -> "dns" :: Nil
    ) :: Nil
  )

  val frontend = Application(
    Name("frontend") :: App("guestbook") :: Tier("frontend") :: Nil,
    Container(
      Name("php-redis") :: Nil,
      image = "gcr.io/google-samples/gb-frontend:v4",
      ports = TCP(80) :: Nil,
      envs = "GET_HOSTS_FROM" -> "dns" :: Nil
    ) :: Nil
  )

  import iat.kubernetes.dsl.NetworkPolicy.ops._

  // external traffic - from external sources
  val connExtFront = frontend
    .communicatesWith(
      Peer.any.withIPs(IP.Range("0.0.0.0/0")).withPorts(frontend.allPorts: _*)
    )
    .ingressOnly
    .named("external-frontend")

  // internal traffic - between components
  val connFrontRedis = frontend
    .communicatesWith(redisMaster)
    .egressOnly
    .labeled(Name("front-redis") :: App("guestbook") :: Nil)
  val connRedisMS = redisMaster
    .communicatesWith(redisSlave)
    .labeled(Name("redis-master-slave") :: App("guestbook") :: Nil)
  val connRedisSM = redisSlave
    .communicatesWith(redisMaster)
    .labeled(Name("redis-slave-master") :: App("guestbook") :: Nil)

  // cluster traffic - to in-cluster services
  val connFrontDns = frontend
    .communicatesWith(NetworkPolicy.peer.kubernetesDns)
    .egressOnly
    .named("front-k8s-dns")
  val connRedisSlaveDns = redisSlave
    .communicatesWith(NetworkPolicy.peer.kubernetesDns)
    .egressOnly
    .named("redis-slave-k8s-dns")

  import iat.skuber.details._

  val redisMasterDetails = resourceRequirements(
    Resource.Requirements(
      requests = Map(
        "cpu" -> Quantity("100m"),
        "memory" -> Quantity("100Mi")
      )
    )
  )

  val redisSlaveDetails = resourceRequirements(
    Resource.Requirements(
      requests = Map(
        "cpu" -> Quantity("100m"),
        "memory" -> Quantity("100Mi")
      )
    )
  ).andThen(
    replicas(2)
  )

  // format: off
  val frontendDetails = (
    serviceType(Service.Type.NodePort),
    resourceRequirements(
      Resource.Requirements(
        requests = Map(
          "cpu" -> Quantity("100m"),
          "memory" -> Quantity("100Mi")
        )
      )
    ).andThen(
      replicas(3)
    )
  )

  import iat.skuber.deployment._
  import skuber.json.format._

  val ns: Seq[Summary] =
    guestbook.interpret.upsert.summary :: Nil
  val apps: Seq[Summary] = List(
    redisMaster
      .interpretWith(guestbook)
      .map(redisMasterDetails),
    redisSlave
      .interpretWith(guestbook)
      .map(redisSlaveDetails),
    frontend
      .interpretWith(guestbook)
      .map(frontendDetails)
    ).flatMap(_.upsert.summary)

  val conns: Seq[Summary] = List(
    NetworkPolicy.default.denyAll.interpretWith(guestbook),
    connExtFront.interpretWith(guestbook),
    connFrontRedis.interpretWith(guestbook),
    connRedisMS.interpretWith(guestbook),
    connRedisSM.interpretWith(guestbook),
    connFrontDns.interpretWith(guestbook),
    connRedisSlaveDns.interpretWith(guestbook)
  ).map(_.upsert.summary)

  (ns ++ apps ++ conns).foreach(s => println(s.asString))

  // Cleanup
  close()
}
