package controllers

import modules.requests._
import renesca._
import model.WustSchema._

object Goals extends NestedNodes[Goal] {
  override def nodeSchema = NodeSchema(routePath, Goal, Map(
    "goals" -> EndConnectSchema(SubGoal),
    "problems" -> EndConnectSchema(Prevents),
    "ideas" -> EndHyperConnectSchema(Achieves, Map(
      "pros" -> EndConnectSchema(SupportsAchievement),
      "cons" -> EndConnectSchema(OpposesAchievement)
    ))
  ))
}
