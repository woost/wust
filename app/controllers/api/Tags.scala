package controllers.api

import controllers.api.nodes.Nodes
import model.WustSchema._
import modules.db.access.{StartContentRelationAccess, ContentNodeAccess, StartRelationRead}
import modules.requests.dsl._

object Tags extends Nodes[Tag] {
  val node = N(Tag, ContentNodeAccess(Tag),
    ("posts", N --> StartRelationRead(Categorizes, Post))
  )
}
