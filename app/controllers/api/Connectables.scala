package controllers.api

import controllers.api.nodes.Nodes
import model.WustSchema.{Tags => SchemaTags, _}
import modules.db.access._
import modules.db.access.custom._
import modules.requests.dsl._

//TODO: should connectable and post share the same api? if so, rename to connectable
object Connectables extends Nodes[Connectable] {
  val node = NodeDef(ConnectableAccess.apply + TaggedTaggable.apply[Connectable],
    "connects-from" -> (N < Connects < (EndConRelationAccess(Connects, Connectable) + ConnectableAccess.apply + TaggedTaggable.apply[Connectable],
      "connects-to" -> (N > StartConRelationAccess(Connects, Connectable) + ConnectableAccess.apply + TaggedTaggable.apply[Connectable]),
      "connects-from" -> (N < EndConRelationAccess(Connects, Connectable) + ConnectableAccess.apply + TaggedTaggable.apply[Connectable])
    )),
    "connects-to" -> (N > Connects > (StartConRelationAccess(Connects, Connectable) + ConnectableAccess.apply + TaggedTaggable.apply[Connectable],
      "connects-to" -> (N > StartConRelationAccess(Connects, Connectable) + ConnectableAccess.apply + TaggedTaggable.apply[Connectable]),
      "connects-from" -> (N < EndConRelationAccess(Connects, Connectable) + ConnectableAccess.apply + TaggedTaggable.apply[Connectable]),
      "up" -> (N < VotesConnectsAccess(1)),
      "neutral" -> (N < VotesConnectsAccess(0))
    ))
  )
}

object ConnectsCtrl extends Nodes[Connects] {
  val node = NodeDef(NodeRead(Connects) + ClassifiedConnects.apply[Connects],
    "classified" -> (N < EndConRelationAccess(Classifies, Classification))
  )
}

object Posts extends Nodes[Post] {
  val node = NodeDef(PostAccess.apply,
    "requests-edit" -> (N < PostUpdatedAccess.apply),
    "requests-tags" -> (N <> PostTagChangeRequestAccess.apply),
    "tags" -> (N < SchemaTags < (EndRelationRead(SchemaTags, Scope),
      "up" -> (N < VotesTagsAccess(1)),
      "neutral" -> (N < VotesTagsAccess(0))
    ))
  )
}
