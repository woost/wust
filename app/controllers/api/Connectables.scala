package controllers.api

import controllers.api.nodes.Nodes
import model.WustSchema.{Tags => SchemaTags, _}
import modules.db.access._
import modules.db.access.custom._
import modules.requests.dsl._

//TODO: should connectable and post share the same api? if so, rename to connectable
object Connectables extends Nodes[Connectable] {
  val node = NodeDef(Connectable, ConnectableAccess.apply + TaggedTaggable.apply[Connectable],
    ("connects-from", N < Connects < (EndContentRelationAccess(Connects, Connectable) + ConnectableAccess.apply + TaggedTaggable.apply[Connectable],
      ("connects-to", N > StartContentRelationAccess(Connects, Connectable) + ConnectableAccess.apply + TaggedTaggable.apply[Connectable]),
      ("connects-from", N < EndContentRelationAccess(Connects, Connectable) + ConnectableAccess.apply + TaggedTaggable.apply[Connectable])
    )),
    ("connects-to", N > Connects > (StartContentRelationAccess(Connects, Connectable) + ConnectableAccess.apply + TaggedTaggable.apply[Connectable],
      ("connects-to", N > StartContentRelationAccess(Connects, Connectable) + ConnectableAccess.apply + TaggedTaggable.apply[Connectable]),
      ("connects-from", N < EndContentRelationAccess(Connects, Connectable) + ConnectableAccess.apply + TaggedTaggable.apply[Connectable])
    )),
    ("tags", N < SchemaTags < (EndRelationRead(SchemaTags, TagLike)))
  )
}
