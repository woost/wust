package controllers.api

import controllers.api.nodes.Nodes
import model.{WustSchema => schema}
import modules.db.access.StartRelationRead
import modules.db.access.custom.{TagAccess, _}
import modules.requests.dsl._

//TODO rename to scope
object Tags extends Nodes[schema.Scope] {
  val node = NodeDef("Scope", TagAccess.apply,
    "posts" -> (N > StartRelationRead(schema.Tags, schema.Post)),
    "inherits" -> (N > StartConRelationAccess(schema.Inherits, schema.Scope)),
    "implements" -> (N < EndConRelationAccess(schema.Inherits, schema.Scope))
  )
}
