package controllers.api

import controllers.api.nodes.Nodes
import model.WustSchema._
import modules.db.access._
import modules.db.access.custom._
import modules.requests.dsl._

object ChangeRequests extends Nodes[ChangeRequest] {
  val node = NodeDef("ChangeRequests", InstantChangeRequestAccess(),
    "up" -> (N < VotesChangeRequestAccess(1)),
    "down" -> (N < VotesChangeRequestAccess(-1)),
    "neutral" -> (N < VotesChangeRequestAccess(0)),
    "skipped" -> (N < ChangeRequestsSkippedAccess())
  )
}
