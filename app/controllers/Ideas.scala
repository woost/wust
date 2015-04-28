package controllers

import modules.requests._
import renesca._
import model.WustSchema._

object Ideas extends NestedNodes[Idea] {
  override def nodeSchema = NodeSchema(routePath, Idea, Map(
    "goals" -> StartHyperConnectSchema(Achieves, Map(
      "pros" -> EndConnectSchema(SupportsAchievement),
      "cons" -> EndConnectSchema(OpposesAchievement)
    )),
    "problems" -> StartHyperConnectSchema(Solves, Map(
      "pros" -> EndConnectSchema(SupportsSolution),
      "cons" -> EndConnectSchema(OpposesSolution)
    )),
    "ideas" -> EndConnectSchema(SubIdea)
  ))
}
