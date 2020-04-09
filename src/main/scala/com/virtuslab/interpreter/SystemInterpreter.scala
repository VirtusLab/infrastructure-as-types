package com.virtuslab.interpreter

import com.virtuslab.dsl.{ DistributedSystem, SystemBuilder }
import com.virtuslab.exporter.skuber.Resource
import _root_.skuber.ObjectResource

class SystemInterpreter[Ctx <: Context](system: DistributedSystem[Ctx]) {
  def resources: Seq[Resource[ObjectResource]] = {
    system.namespaces.flatMap { ns =>
      Seq(ns.interpret()) ++ ns.obj.members.toSeq.map(_.interpret())
    }
  }.toSeq.asInstanceOf[Seq[Resource[ObjectResource]]]
}

object SystemInterpreter {
  def apply[Ctx <: Context](system: DistributedSystem[Ctx]): SystemInterpreter[Ctx] = new SystemInterpreter(system)
  def of[Ctx <: Context](system: DistributedSystem[Ctx]): SystemInterpreter[Ctx] = new SystemInterpreter(system)
  def of[Ctx <: Context](builder: SystemBuilder[Ctx]): SystemInterpreter[Ctx] = of(builder.build())
}
