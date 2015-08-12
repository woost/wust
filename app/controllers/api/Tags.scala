package controllers.api

import controllers.api.nodes.Nodes
import model.WustSchema._
import modules.db.access.StartRelationRead
import modules.db.access.custom.{TagAccess, TaggedTaggable}
import modules.requests.dsl._

object Tags extends Nodes[TagLike] {
  val node = NodeDef(TagLikeMatches, TagAccess.apply,
    ("posts", N > StartRelationRead(Categorizes, Post) + TaggedTaggable.apply[Taggable])
  )
}
