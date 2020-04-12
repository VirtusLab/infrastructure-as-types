package com.virtuslab.interpreter

import com.virtuslab.dsl.{ DistributedSystem, Namespace, RootDefinition, SystemBuilder }

class SystemInterpreter[Ctx <: Context](system: RootDefinition[Ctx, DistributedSystem, Namespace]) {
  def resources: Iterable[Ctx#Ret] = {
    system.interpret() ++ system.members.flatMap { ns =>
      ns.interpret() ++ ns.members.flatMap(_.interpret())
    }
  }
}

object SystemInterpreter {
  def apply[Ctx <: Context](system: RootDefinition[Ctx, DistributedSystem, Namespace]): SystemInterpreter[Ctx] =
    new SystemInterpreter(system)
  def of[Ctx <: Context](system: RootDefinition[Ctx, DistributedSystem, Namespace]): SystemInterpreter[Ctx] =
    new SystemInterpreter(system)
  def of[Ctx <: Context](
      builder: SystemBuilder[Ctx]
    )(implicit
      ctx: Ctx,
      ev: RootInterpreter[Ctx, DistributedSystem, Namespace]
    ): SystemInterpreter[Ctx] = of(builder.build())
}
