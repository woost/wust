package controllers.api

import controllers.api.nodes.Nodes
import model.WustSchema._
import modules.db.access.{ContentNodeAccess, EndContentRelationAccess, StartContentRelationAccess}
import modules.requests._

object Ideas extends Nodes[Idea] {
  lazy val nodeSchema = NodeSchema(routePath, new ContentNodeAccess(Idea),
    Map(
      "goals" -> StartHyperConnectSchema(Achieves, new StartContentRelationAccess(Achieves, Goal), Map(
        "pros" -> EndConnectSchema(new EndContentRelationAccess(SupportsAchievement, ProArgument)),
        "cons" -> EndConnectSchema(new EndContentRelationAccess(OpposesAchievement, ConArgument))
      )),
      "problems" -> StartHyperConnectSchema(Solves, new StartContentRelationAccess(Solves, Problem), Map(
        "pros" -> EndConnectSchema(new EndContentRelationAccess(SupportsSolution, ProArgument)),
        "cons" -> EndConnectSchema(new EndContentRelationAccess(OpposesSolution, ConArgument))
      )),
      "ideas" -> EndConnectSchema(new EndContentRelationAccess(SubIdea, Idea))
    )
  )
}
