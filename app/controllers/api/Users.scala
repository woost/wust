package controllers.api

import controllers.api.nodes.Nodes
import model.WustSchema.ContentNodeMatches
import model.{WustSchema => schema}
import modules.db.access.{StartRelationRead,StartMultiRelationRead}
import modules.db.access.custom.UserAccess
import modules.requests.dsl._

object Users extends Nodes[schema.User] {
  val node = NodeDef(schema.UserMatches, UserAccess.apply,
    ("created", N > StartRelationRead(schema.Created, ContentNodeMatches)),
    ("updated", N > StartRelationRead(schema.Updated, ContentNodeMatches)),
    ("deleted", N > StartRelationRead(schema.Deleted, ContentNodeMatches)),
    ("contributions", N > StartMultiRelationRead(schema.Created, schema.Updated, schema.Deleted)(ContentNodeMatches))
  )
}
