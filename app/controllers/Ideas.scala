package controllers

import modules.requests._
import renesca._
import model.WustSchema._

object Ideas extends NestedNodes[Idea] {
  override def nodeSchema = NodeSchema(routePath, Idea, Map(
    ("goals" ,StartHyperConnectSchema(Achieves, Goal, Map(
      ("pros" , EndConnectSchema(SupportsAchievement, ProArgument)),
      ("cons" , EndConnectSchema(OpposesAchievement, ConArgument))
    ))),
    "problems" -> StartHyperConnectSchema(Solves, Problem, Map(
      "pros" -> EndConnectSchema(SupportsSolution, ProArgument),
      "cons" -> EndConnectSchema(OpposesSolution, ConArgument)
    )),
    "ideas" -> EndConnectSchema(SubIdea, Idea)
  ))
}
