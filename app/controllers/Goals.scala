package controllers

import modules.requests._
import renesca._
import model.WustSchema._

object Goals extends NestedNodes[Goal] {
  override def nodeSchema = NodeSchema(routePath, Goal, Map(
    "goals" -> EndConnectSchema(SubGoal, Goal),
    "problems" -> EndConnectSchema(Prevents, Problem),
    "ideas" -> EndHyperConnectSchema(Achieves, Idea, Map(
      "pros" -> EndConnectSchema(SupportsAchievement, ProArgument),
      "cons" -> EndConnectSchema(OpposesAchievement, ConArgument)
    ))
  ))
}
