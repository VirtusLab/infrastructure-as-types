package com.virtuslab.dsl.interpreter

import com.virtuslab.dsl.Application.DefinedApplication
import com.virtuslab.dsl.Configuration.DefinedConfiguration
import com.virtuslab.dsl.DistributedSystem.DefinedDistributedSystem
import skuber.ObjectResource

class SystemInterpreter(
    applicationInterpreters: PartialFunction[
      DefinedApplication,
      ApplicationInterpreter
    ],
    config: ConfigurationInterpreter,
    namespace: NamespaceInterpreter) {

  def apply(system: DefinedDistributedSystem): Seq[ObjectResource] = {
    system.namespaces.flatMap { ns =>
      Seq(namespace(ns)) ++ ns.members.toSeq.flatMap {
        case app: DefinedApplication =>
          if (applicationInterpreters.isDefinedAt(app)) {
            val (svc, dpl) = applicationInterpreters(app)(app)
            Seq(svc, dpl)
          } else {
            throw new IllegalArgumentException(
              s"Application $app is not suitable for the interpreter."
            )
          }
        case cfg: DefinedConfiguration =>
          Seq(config(cfg))
      }
    }
  }.toSeq
}

object SystemInterpreter {
  def apply(
      applicationInterpreters: PartialFunction[
        DefinedApplication,
        ApplicationInterpreter
      ],
      configurationInterpreter: ConfigurationInterpreter,
      namespaceInterpreter: NamespaceInterpreter
    ): SystemInterpreter = new SystemInterpreter(applicationInterpreters, configurationInterpreter, namespaceInterpreter)

  def of(system: DefinedDistributedSystem): SystemInterpreter = {
    val applicationInterpreter = new ApplicationInterpreter(system)
    new SystemInterpreter({
      case _: DefinedApplication => applicationInterpreter
    }, new ConfigurationInterpreter, new NamespaceInterpreter)
  }
}
