package com.virtuslab.iat.scala

trait UnitOps {
  implicit class UnitCastOps(self: Any) {

    /** Explicit cast to Unit to avoid wartremover:NonUnitStatements when discarding a non-Unit value is intended
      * (typically when calling side-effecting methods from Java APIs). */
    def toUnit(): Unit = ()
  }
}
