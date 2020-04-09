package com.virtuslab.dsl.interpreter

import com.virtuslab.dsl.DistributedSystem.DistributedSystemDefinition
import com.virtuslab.dsl.SystemBuilder
import com.virtuslab.exporter.skuber.Resource
import skuber.ObjectResource

class SystemInterpreter(
    system: DistributedSystemDefinition,
    applicationInterpreter: ApplicationInterpreter,
    namespace: NamespaceInterpreter) {

  def resources: Seq[Resource[ObjectResource]] = {
    import skuber.json.format._
    system.namespaces.flatMap { ns =>
      Seq(Resource(namespace(ns))) ++ ns.members.toSeq.map(_.interpret())
//        .flatMap {
//          case app: ApplicationDefinition =>
//            val (svc, dpl) = applicationInterpreter(app)
//            Seq(Resource(svc), Resource(dpl))
//          case cfg @ Definition(_: Configuration, _) =>
//            val interpret = implicitly[Interpreter[Definition[Configuration], ConfigMap]]
//            val configMap = interpret(cfg.asInstanceOf[Definition[Configuration]])
//            Seq(Resource(configMap))
//            cfg.interpret().mat
//          case cnn: ConnectionDefinition =>
//            Seq(Resource(connection(cnn)))
//          case o =>
//            println("No interpreter for: " + o)
//            Seq.empty
//        }
    }
  }.toSeq.asInstanceOf[Seq[Resource[ObjectResource]]]
}

object SystemInterpreter {
  def apply(
      system: DistributedSystemDefinition,
      applicationInterpreter: ApplicationInterpreter,
      namespaceInterpreter: NamespaceInterpreter
    ): SystemInterpreter = new SystemInterpreter(
    system,
    applicationInterpreter,
    namespaceInterpreter
  )

  def of(system: DistributedSystemDefinition): SystemInterpreter = {
    new SystemInterpreter(
      system,
      new ApplicationInterpreter(MountInterpreter, system),
      NamespaceInterpreter
    )
  }

  def of(builder: SystemBuilder): SystemInterpreter = {
    of(builder.build())
  }
}
