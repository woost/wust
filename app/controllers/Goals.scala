package controllers

import modules.requests._
import renesca._
import model.WustSchema._

object Goals extends ContentNodesController[Goal] {
  override def nodeSchema = NodeSchema("goals", Goal, Map(
    "goals" -> EndConnectSchema(SubGoal),
    "problems" -> EndConnectSchema(Prevents),
    "ideas" -> EndConnectSchema(Achieves)
  ))
}
