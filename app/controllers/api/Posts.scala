package controllers.api

import controllers.api.nodes.Nodes
import model.WustSchema.{Tags => SchemaTags, _}
import modules.db.access._
import modules.db.access.custom._
import modules.requests.dsl._

object Posts extends Nodes[Post] {
  val node = NodeDef(PostAccess.apply + TaggedTaggable.apply[Post],
    "requests-edit" -> (N < PostUpdatedAccess.apply),
    "requests-add-tags" -> (N < PostAddTagsAccess.apply + TaggedTaggable.apply[AddTags]),
    "requests-remove-tags" -> (N < PostRemoveTagsAccess.apply + TaggedTaggable.apply[RemoveTags])
  )
}
