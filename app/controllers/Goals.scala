package controllers

import controllers.nodes._
import model.WustSchema._
import modules.db.access.{ContentNodeAccess, EndContentRelationAccess}
import modules.requests._

object Goals extends Nodes[Goal] {
  lazy val nodeSchema = NodeSchema(routePath, new ContentNodeAccess(Goal),
    Map(
      "goals" -> EndConnectSchema(new EndContentRelationAccess(SubGoal, Goal)),
      "problems" -> EndConnectSchema(new EndContentRelationAccess(Prevents, Problem)),
      "ideas" -> EndHyperConnectSchema(Achieves, new EndContentRelationAccess(Achieves, Idea), Map(
        "pros" -> EndConnectSchema(new EndContentRelationAccess(SupportsAchievement, ProArgument)),
        "cons" -> EndConnectSchema(new EndContentRelationAccess(OpposesAchievement, ConArgument))
      ))
    )
  )
}
