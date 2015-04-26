package controllers

import modules.requests._
import renesca._
import model.WustSchema._

object Ideas extends ContentNodesController[Idea] {
  override def nodeSchema = NodeSchema("ideas", Idea, Map(
    "goals" -> StartHyperConnectSchema(Achieves, Map(
      "pro" -> EndConnectSchema(SupportsAchievement),
      "con" -> EndConnectSchema(OpposesAchievement)
    )),
    "problems" -> StartHyperConnectSchema(Solves, Map(
      "pro" -> EndConnectSchema(SupportsSolution),
      "con" -> EndConnectSchema(OpposesSolution)
    )),
    "ideas" -> EndConnectSchema(SubIdea)
  ))
}
