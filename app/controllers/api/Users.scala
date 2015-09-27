package controllers.api

import controllers.api.nodes.Nodes
import model.{WustSchema => schema}
import modules.db.access.custom._
import modules.requests.dsl._

object Users extends Nodes[schema.User] {
  val node = NodeDef(UserAccess.apply,
    "contributions" -> (N <> UserContributions.apply),
    "karma-contexts" -> (N > UserHasKarmaScopes.apply),
    "karma-log" -> (N > UserHasKarmaLog.apply),
    "marks" -> (N > UserMarks.apply)
  )
}
