package controllers.api

import controllers.api.nodes.Nodes
import model.WustSchema._
import modules.db.access._
import modules.db.access.custom._
import modules.requests.dsl._

object InstantRequests extends Nodes[ChangeRequest] {
  val node = NodeDef("InstantRequests", InstantChangeRequestAccess())
}

object EditRequests extends Nodes[Updated] {
  val node = NodeDef("RequestsEdit", NodeNothing(Updated),
    "up" -> (N < VotesUpdatedAccess(1)),
    "down" -> (N < VotesUpdatedAccess(-1)),
    "neutral" -> (N < VotesUpdatedAccess(0))
  )
}

object TagsRequests extends Nodes[TagChangeRequest] {
  val node = NodeDef("RequestsTags", NodeNothing(TagChangeRequest),
    "up" -> (N < VotesTagsChangeRequestAccess(1)),
    "down" -> (N < VotesTagsChangeRequestAccess(-1)),
    "neutral" -> (N < VotesTagsChangeRequestAccess(0))
  )
}
