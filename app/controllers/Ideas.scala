package controllers

import modules.requests._
import renesca._
import model.WustSchema._

object Ideas extends ContentNodesController[Idea] {
  override def nodeSchema = NodeSchema("ideas", Idea, Map(
    "goals" -> StartConnectSchema(Achieves),
    "problems" -> StartConnectSchema(Solves),
    "ideas" -> EndConnectSchema(SubIdea)
  ))
}
