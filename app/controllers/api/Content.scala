package controllers.api

import controllers.api.nodes.Nodes
import model.WustSchema.{Tags => SchemaTags, _}
import modules.db.access._
import modules.db.access.custom._
import modules.requests.dsl._
import formatters.json.PostFormat.PostFormat
import formatters.json.TagFormat.ScopeFormat

object Connectables extends Nodes[Connectable] {
  val node = NodeDef(NodeNothing(Connectable),
    "connects-from" -> (N < Connects <(EndConnectsAccess.apply,
      "connects-from" -> (N < EndConnectsAccess.apply)
      ))
  )
}

object References extends Nodes[Reference] {
  val node = NodeDef(ReferenceAccess.apply)
}

object Posts extends Nodes[Post] {
  val node = NodeDef(PostAccess.apply,
    "requests" -> (N <> PostChangeRequestAccess.apply),
    "tags" -> (N < SchemaTags <(EndRelationRead(SchemaTags, Scope),
      "up" -> (N < VotesTagsAccess(1)),
      "neutral" -> (N < VotesTagsAccess(0))
      )),
    "connects-to" -> (N > Connects >(StartConnectsAccess.apply,
      "connects-from" -> (N < EndConnectsAccess.apply),
      "up" -> (N < VotesConnectsAccess(1)),
      "neutral" -> (N < VotesConnectsAccess(0))
      ))
  )
}

object Scopes extends Nodes[Scope] {
  val node = NodeDef(TagAccess.apply,
    "posts" -> (N > StartRelationRead(SchemaTags, Post)),
    "inherits" -> (N > StartConRelationAccess(Inherits, Scope)),
    "implements" -> (N < EndConRelationAccess(Inherits, Scope))
  )
}
