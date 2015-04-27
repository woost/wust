package controllers

import modules.requests._
import renesca._
import model.WustSchema._

object Goals extends ContentNodes[Goal] with NestedNodes[Goal] {
  override def nodeSchema = NodeSchema("goals", Goal, Map(
    "goals" -> EndConnectSchema(SubGoal),
    "problems" -> EndConnectSchema(Prevents),
    "ideas" -> EndHyperConnectSchema(Achieves, Map(
      "pros" -> EndConnectSchema(SupportsAchievement),
      "cons" -> EndConnectSchema(OpposesAchievement)
    ))
  ))
}
