package com.virtuslab.dsl.interpreter

import com.virtuslab.dsl.Application.ApplicationDefinition
import com.virtuslab.dsl.Configuration.ConfigurationDefinition
import com.virtuslab.dsl.Connection.ConnectionDefinition
import com.virtuslab.dsl.DistributedSystem.DistributedSystemDefinition
import com.virtuslab.dsl.SystemBuilder
import com.virtuslab.internal.SkuberConverter.Resource
import skuber.ObjectResource

class SystemInterpreter(
    system: DistributedSystemDefinition,
    applicationInterpreters: PartialFunction[
      ApplicationDefinition,
      ApplicationInterpreter
    ],
    config: ConfigurationInterpreter,
    connection: ConnectionInterpreter,
    namespace: NamespaceInterpreter) {

  def resources: Seq[Resource[ObjectResource]] = {
    import skuber.json.format._
    system.namespaces.flatMap { ns =>
      Seq(Resource(namespace(ns))) ++ ns.members.toSeq
        .flatMap {
          case app: ApplicationDefinition =>
            if (applicationInterpreters.isDefinedAt(app)) {
              val (svc, dpl) = applicationInterpreters(app)(app)
              Seq(Resource(svc), Resource(dpl))
            } else {
              throw new IllegalArgumentException(
                s"Application $app is not suitable for the interpreter."
              )
            }
          case cfg: ConfigurationDefinition =>
            Seq(Resource(config(cfg)))
          case cnn: ConnectionDefinition =>
            Seq(Resource(connection(cnn)))
          case o =>
            println("No interpreter for: " + o)
            Seq.empty
        }
    }
  }.toSeq.asInstanceOf[Seq[Resource[ObjectResource]]]
}

object SystemInterpreter {
  def apply(
      system: DistributedSystemDefinition,
      applicationInterpreters: PartialFunction[
        ApplicationDefinition,
        ApplicationInterpreter
      ],
      configurationInterpreter: ConfigurationInterpreter,
      connectionInterpreter: ConnectionInterpreter,
      namespaceInterpreter: NamespaceInterpreter
    ): SystemInterpreter = new SystemInterpreter(
    system,
    applicationInterpreters,
    configurationInterpreter,
    connectionInterpreter,
    namespaceInterpreter
  )

  def of(system: DistributedSystemDefinition): SystemInterpreter = {
    new SystemInterpreter(
      system, {
        case _: ApplicationDefinition => new ApplicationInterpreter(system)
      },
      new ConfigurationInterpreter,
      new ConnectionInterpreter(
        new LabelExpressionInterpreter(),
        new NetworkPortsInterpreter()
      ),
      new NamespaceInterpreter
    )
  }

  def of(builder: SystemBuilder): SystemInterpreter = {
    of(builder.build())
  }
}
