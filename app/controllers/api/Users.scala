package controllers.api

import controllers.api.nodes.Nodes
import model.{WustSchema => schema}
import modules.db.access.custom._
import modules.db.access.StartRelationWrite
import modules.requests.dsl._
import formatters.json.PostFormat.PostFormat

object Users extends Nodes[schema.User] {
  val node = NodeDef(UserAccess.apply,
    "contributions" -> (N <> UserContributions.apply),
    "karma-contexts" -> (N > UserHasKarmaScopes.apply),
    "karma-log" -> (N > UserHasKarmaLog.apply),
    "marks" -> (N > UserMarks.apply + CheckOwnUser.apply),
    "history" -> (N > UserHasHistory.apply + CheckOwnUser.apply),
    "follows" -> (N > StartRelationWrite(schema.Follows, schema.Post)),
    "notifications" -> (N <> UserHasNotifications.apply)
  )
}
