package controllers

import controllers.nodes.Nodes
import model.WustSchema._
import modules.db.access.{ContentNodeAccess, EndContentRelationAccess, StartContentRelationAccess}
import modules.requests._

object Problems extends Nodes[Problem] {
  lazy val nodeSchema = NodeSchema(routePath, new ContentNodeAccess(Problem), Map(
    "goals" -> StartConnectSchema(new StartContentRelationAccess(Prevents, Goal)),
    "causes" -> StartConnectSchema(new StartContentRelationAccess(Causes, Problem)),
    "consequences" -> EndConnectSchema(new EndContentRelationAccess(Causes, Problem)),
    "ideas" -> EndHyperConnectSchema(Solves, new EndContentRelationAccess(Solves, Idea), Map(
      "pros" -> EndConnectSchema(new EndContentRelationAccess(SupportsSolution, ProArgument)),
      "cons" -> EndConnectSchema(new EndContentRelationAccess(OpposesSolution, ConArgument))
    ))
  ))
}
