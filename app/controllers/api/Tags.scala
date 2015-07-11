package controllers.api

import controllers.api.nodes.Nodes
import model.WustSchema._
import modules.db.access.custom.ContentNodeAccess
import modules.db.access.StartRelationRead
import modules.requests.dsl._

object Tags extends Nodes[Tag] {
  val node = NodeDef(Tag, ContentNodeAccess(Tag),
    ("posts", N > StartRelationRead(Categorizes, Post))
  )
}
