package controllers.api

import controllers.api.nodes.Nodes
import model.WustSchema._
import modules.db.access.{EndContentRelationAccess, ContentNodeAccess, StartContentRelationAccess}
import modules.requests._

//TODO nodeschema should have type NodeSchema[Untyped] not ContentNode...
object Untypeds extends Nodes[ContentNode] {
  lazy val nodeSchema = NodeSchema(routePath, new ContentNodeAccess(Untyped), Map(
    "responds" -> StartConnectSchema(new StartContentRelationAccess(Refers, Untyped)),
    "prays" -> EndConnectSchema(new EndContentRelationAccess(Refers, Untyped))
  ))
}
