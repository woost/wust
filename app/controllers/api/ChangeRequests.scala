package controllers.api

import controllers.api.nodes.Nodes
import model.WustSchema._
import modules.db.access._
import modules.db.access.custom._
import modules.requests.dsl._

//TODO: one api for both?
object InstantRequests extends Nodes[ChangeRequest] {
  val node = NodeDef("InstantRequests", InstantChangeRequestAccess(),
    "skipped" -> (N < ChangeRequestsSkippedAccess())
  )
}

object ChangeRequests extends Nodes[ChangeRequest] {
  val node = NodeDef("ChangeRequests", NodeNothing(ChangeRequest),
    "up" -> (N < VotesChangeRequestAccess(1)),
    "down" -> (N < VotesChangeRequestAccess(-1)),
    "neutral" -> (N < VotesChangeRequestAccess(0))
  )
}
