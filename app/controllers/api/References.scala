package controllers.api

import controllers.api.nodes.Nodes
import model.WustSchema._
import modules.db.access._
import modules.db.access.custom._
import modules.requests.dsl._

object References extends Nodes[Reference] {
  val node = NodeDef(NodeRead(Reference),
    "up" -> (N < VotesReferenceAccess(1)),
    "down" -> (N < VotesReferenceAccess(-1)),
    "neutral" -> (N < VotesReferenceAccess(0))
  )
}
