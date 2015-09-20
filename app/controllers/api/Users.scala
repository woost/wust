package controllers.api

import controllers.api.nodes.Nodes
import model.{WustSchema => schema}
import modules.db.access.{StartRelationRead,StartMultiRelationRead}
import modules.db.access.custom.{UserAccess, UserContributions}
import modules.requests.dsl._

object Users extends Nodes[schema.User] {
  val node = NodeDef(UserAccess.apply,
    "created" -> (N > StartRelationRead(schema.Created, schema.Post)),
    "updated" -> (N > StartRelationRead(schema.Updated, schema.Post)),
    // "deleted" -> (N > StartRelationRead(schema.Deleted, schema.Post))
    "contributions" -> (N <> UserContributions.apply)
  )
}
