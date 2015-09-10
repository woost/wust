package controllers.api

import controllers.api.nodes.Nodes
import model.WustSchema._
import modules.db.access._
import modules.db.access.custom._
import modules.requests.dsl._

object EditRequests extends Nodes[Updated] {
  val node = NodeDef("RequestsEdit", NodeRead(Updated),
    "up" -> (N < VotesUpdatedAccess(1)),
    "down" -> (N < VotesUpdatedAccess(-1)),
    "neutral" -> (N < VotesUpdatedAccess(0))
  )
}

object AddTagsRequests extends Nodes[AddTags] {
  val node = NodeDef("RequestsAddTag", NodeRead(AddTags) + TaggedTaggable.apply[AddTags],
    "up" -> (N < VotesAddTagsAccess(1)),
    "down" -> (N < VotesAddTagsAccess(-1)),
    "neutral" -> (N < VotesAddTagsAccess(0))
  )
}

object RemoveTagsRequests extends Nodes[RemoveTags] {
  val node = NodeDef("RequestsRemoveTag", NodeRead(RemoveTags) + TaggedTaggable.apply[RemoveTags],
    "up" -> (N < VotesRemoveTagsAccess(1)),
    "down" -> (N < VotesRemoveTagsAccess(-1)),
    "neutral" -> (N < VotesRemoveTagsAccess(0))
  )
}
