package com.virtuslab.interpreter

import com.virtuslab.dsl.{ DistributedSystem, SystemBuilder }

class SystemInterpreter[Ctx <: Context](system: DistributedSystem[Ctx]) {
  def resources: Seq[Ctx#Ret] = {
    system.namespaces.flatMap { ns =>
      ns.interpret() ++ ns.obj.members.toSeq.flatMap(_.interpret())
    }
  }.toSeq
}

object SystemInterpreter {
  def apply[Ctx <: Context](system: DistributedSystem[Ctx]): SystemInterpreter[Ctx] = new SystemInterpreter(system)
  def of[Ctx <: Context](system: DistributedSystem[Ctx]): SystemInterpreter[Ctx] = new SystemInterpreter(system)
  def of[Ctx <: Context](builder: SystemBuilder[Ctx]): SystemInterpreter[Ctx] = of(builder.build())
}
