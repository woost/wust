package controllers.api

import controllers.api.nodes.Nodes
import model.WustSchema.ContentNodeMatches
import model.{WustSchema => schema}
import modules.db.access.StartRelationRead
import modules.db.access.custom.UserAccess
import modules.requests.dsl._

object Users extends Nodes[schema.User] {
  implicit val restFormat = formatters.json.UserFormats.RestFormat

  val node = NodeDef(schema.User, UserAccess.apply,
    ("created", N > StartRelationRead(schema.Created, ContentNodeMatches)),
    ("updated", N > StartRelationRead(schema.Updated, ContentNodeMatches)),
    ("deleted", N > StartRelationRead(schema.Deleted, ContentNodeMatches))
  )
}
