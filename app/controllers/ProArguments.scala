package controllers

import controllers.nodes.Nodes
import controllers.router.NestedResourceRouter
import model.WustSchema._
import modules.db.ContentNodeAccess
import modules.requests._

object ProArguments extends Nodes[ProArgument] {
  lazy val nodeSchema = NodeSchema(routePath, new ContentNodeAccess(ProArgument), Map())
}
