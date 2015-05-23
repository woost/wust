package controllers.api

import controllers.api.nodes.Nodes
import model.WustSchema._
import modules.db.access.{EndContentRelationAccess, ContentNodeAccess, StartContentRelationAccess}
import modules.requests._

object Untypeds extends Nodes[Untyped] {
  lazy val nodeSchema = NodeSchema(routePath, new ContentNodeAccess(Untyped), Map(
    "responds" -> EndConnectSchema(new EndContentRelationAccess(Refers, Untyped)),
    "prays" -> StartConnectSchema(new StartContentRelationAccess(Refers, Untyped))
  ))
}
