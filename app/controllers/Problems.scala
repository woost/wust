package controllers

import modules.requests._
import renesca._
import model.WustSchema._

object Problems extends ContentNodesController[Problem] {
  override def nodeSchema = NodeSchema("problems", Problem, Map(
    "goals" -> StartConnectSchema(Prevents),
    "causes" -> EndConnectSchema(Causes),
    "consequences" -> StartConnectSchema(Causes),
    "ideas" -> EndHyperConnectSchema(Solves, Map(
      "pro" -> EndConnectSchema(SupportsSolution),
      "con" -> EndConnectSchema(OpposesSolution)
    ))
  ))
}

