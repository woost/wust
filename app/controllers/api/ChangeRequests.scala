package controllers.api

import controllers.api.nodes.Nodes
import model.WustSchema._
import modules.db.access._
import modules.db.access.custom._
import modules.requests.dsl._

object EditRequests extends Nodes[Updated] {
  val node = NodeDef(NodeRead(Updated),
    "up" -> (N < VotesUpdatedAccess(1)),
    "down" -> (N < VotesUpdatedAccess(-1)),
    "neutral" -> (N < VotesUpdatedAccess(0))
  )
}

object TagsRequests extends Nodes[UpdatedTags] {
  val node = NodeDef(NodeRead(UpdatedTags) + TaggedTaggable.apply[UpdatedTags],
    "up" -> (N < VotesUpdatedTagsAccess(1)),
    "down" -> (N < VotesUpdatedTagsAccess(-1)),
    "neutral" -> (N < VotesUpdatedTagsAccess(0))
  )
}
