package controllers.api

import controllers.api.nodes.Nodes
import model.WustSchema._
import modules.db.access._
import modules.db.access.custom._
import modules.requests.dsl._

object ChangeRequests extends Nodes[Updated] {
  val node = NodeDef(Updated, NodeRead(Updated),
    ("up", N < VotesOnUpdatedAccess(1) + CheckUser.apply),
    ("down", N < VotesOnUpdatedAccess(-1) + CheckUser.apply),
    ("neutral", N < VotesOnUpdatedAccess(0) + CheckUser.apply)
  )
}
