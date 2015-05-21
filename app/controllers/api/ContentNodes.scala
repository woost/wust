package controllers.api

import controllers.api.nodes.Nodes
import model.WustSchema._
import modules.db.access._
import modules.requests._

object ContentNodes extends Nodes[ContentNode] {
   lazy val nodeSchema = NodeSchema(routePath, new AnyContentNode(), Map(
     "refers" -> StartConnectSchema(new StartAnyRelation(Refers))
   ))
 }
