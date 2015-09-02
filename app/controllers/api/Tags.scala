package controllers.api

import controllers.api.nodes.Nodes
import model.{WustSchema => schema}
import modules.db.access.StartRelationRead
import modules.db.access.custom.{TagAccess, TaggedTaggable}
import modules.requests.dsl._

//TODO: rename to TagLike
object Tags extends Nodes[schema.TagLike] {
  val node = NodeDef(TagAccess.apply,
    ("posts", N > StartRelationRead(schema.Tags, schema.Post) + TaggedTaggable.apply[schema.Taggable])
  )
}
