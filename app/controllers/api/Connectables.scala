package controllers.api

import controllers.api.nodes.Nodes
import model.WustSchema.{Tags => SchemaTags, _}
import modules.db.access._
import modules.db.access.custom._
import modules.requests.dsl._

//TODO: should connectable and post share the same api? if so, rename to connectable
object Connectables extends Nodes[Connectable] {
  // TODO: combine into posts
  val node = NodeDef(NodeRead(Connectable),
    "connects-from" -> (N < Connects < (EndConnectsAccess.apply,
      "connects-from" -> (N < EndConnectsAccess.apply)
    ))
  )
}

object ConnectsCtrl extends Nodes[Connects] {
  val node = NodeDef(ConnectsAccess.apply,
    "classified" -> (N < EndConnectsAccess.apply)
  )
}

object Posts extends Nodes[Post] {
  val node = NodeDef(PostAccess.apply,
    "requests-edit" -> (N < PostUpdatedAccess.apply),
    "requests-tags" -> (N <> PostTagChangeRequestAccess.apply),
    "tags" -> (N < SchemaTags < (EndRelationRead(SchemaTags, Scope),
      "up" -> (N < VotesTagsAccess(1)),
      "neutral" -> (N < VotesTagsAccess(0))
    )),
    "connects-to" -> (N > Connects > (StartConnectsAccess.apply,
      "connects-from" -> (N < EndConnectsAccess.apply),
      "up" -> (N < VotesConnectsAccess(1)),
      "neutral" -> (N < VotesConnectsAccess(0))
    ))
  )
}
