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

object TagsRequests extends Nodes[TagChangeRequest] {
  val node = NodeDef("RequestsTags", TagChangeRequestAccess(TagChangeRequest),
    "up" -> (N < VotesTagsChangeRequestAccess(1)),
    "down" -> (N < VotesTagsChangeRequestAccess(-1)),
    "neutral" -> (N < VotesTagsChangeRequestAccess(0))
  )
}
