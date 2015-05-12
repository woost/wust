package controllers

import modules.requests._
import renesca._
import model.WustSchema._

object Problems extends NestedNodes[Problem] {
  override def nodeSchema = NodeSchema(routePath, Problem, Map(
    "goals" -> StartConnectSchema(Prevents, Goal),
    "causes" -> EndConnectSchema(Causes, Problem),
    "consequences" -> StartConnectSchema(Causes, Problem),
    "ideas" -> EndHyperConnectSchema(Solves, Idea, Map(
      "pros" -> EndConnectSchema(SupportsSolution, ProArgument),
      "cons" -> EndConnectSchema(OpposesSolution, ConArgument)
    ))
  ))
}

