package controllers.api

import controllers.api.nodes.Nodes
import model.WustSchema._
import modules.db.access.{ContentNodeAccess, StartRelationRead}
import modules.requests._

object Tags extends Nodes[Tag] {
  lazy val nodeSchema = NodeSchema(routePath, new ContentNodeAccess(Tag),
    Map(
      "posts" -> StartConnectSchema(new StartRelationRead(CategorizesPost, Post))
    )
  )
}
