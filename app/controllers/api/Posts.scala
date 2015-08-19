package controllers.api

import controllers.api.nodes.Nodes
import model.WustSchema.{Tags => SchemaTags, _}
import modules.db.access._
import modules.db.access.custom._
import modules.requests.dsl._

//TODO: should connectable and post share the same api? if so, rename to connectable
object Posts extends Nodes[Connectable] {
  val node = NodeDef(Connectable, PostAccess.apply + TaggedTaggable.apply[Connectable],
    //TODO: should be TaggedTaggable.apply(Post), but relationaccesscontrols are not covariant and can't be, maybe hack like with relationaccess?
    ("connects-from", N < Connects < (EndContentRelationAccess(Connects, Connectable) + PostAccess.apply + TaggedTaggable.apply[Connectable],
      ("connects-to", N > StartContentRelationAccess(Connects, Connectable) + PostAccess.apply + TaggedTaggable.apply[Connectable]),
      ("connects-from", N < EndContentRelationAccess(Connects, Connectable) + PostAccess.apply + TaggedTaggable.apply[Connectable])
    )),
    ("connects-to", N > Connects > (StartContentRelationAccess(Connects, Connectable) + PostAccess.apply + TaggedTaggable.apply[Connectable],
      ("connects-to", N > StartContentRelationAccess(Connects, Connectable) + PostAccess.apply + TaggedTaggable.apply[Connectable]),
      ("connects-from", N < EndContentRelationAccess(Connects, Connectable) + PostAccess.apply + TaggedTaggable.apply[Connectable])
    )),
    ("votes", N < Dimensionizes < (EndRelationRead(Dimensionizes, VoteDimension),
      ("up", N < VotesAccess(1) + CheckUser.apply),
      ("down", N < VotesAccess(-1) + CheckUser.apply),
      ("neutral", N < VotesAccess(0) + CheckUser.apply)
    )),
    ("tags", N < SchemaTags < (EndRelationRead(SchemaTags, TagLike)))
  )
}
