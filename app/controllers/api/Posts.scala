package controllers.api

import controllers.api.nodes.Nodes
import model.WustSchema.{Tags => SchemaTags, _}
import modules.db.access._
import modules.db.access.custom._
import modules.requests.dsl._

object Posts extends Nodes[Post] {
  val node = NodeDef(Post, PostAccess.apply + TaggedTaggable.apply[Post],
    //TODO: should be TaggedTaggable.apply(Post), but relationaccesscontrols are not covariant and can't be, maybe hack like with relationaccess?
    ("connects-from", N < Connects < (EndContentRelationAccess(Connects, Post) + PostAccess.apply + TaggedTaggable.apply[Connectable],
      ("connects-to", N > StartContentRelationAccess(Connects, Post) + PostAccess.apply + TaggedTaggable.apply[Connectable]),
      ("connects-from", N < EndContentRelationAccess(Connects, Post) + PostAccess.apply + TaggedTaggable.apply[Connectable])
    )),
    ("connects-to", N > Connects > (StartContentRelationAccess(Connects, Post) + PostAccess.apply + TaggedTaggable.apply[Connectable],
      ("connects-to", N > StartContentRelationAccess(Connects, Post) + PostAccess.apply + TaggedTaggable.apply[Connectable]),
      ("connects-from", N < EndContentRelationAccess(Connects, Post) + PostAccess.apply + TaggedTaggable.apply[Connectable])
    )),
    ("votes", N < Dimensionizes < (EndRelationRead(Dimensionizes, VoteDimension),
      ("up", N < VotesAccess(1) + CheckUser.apply),
      ("down", N < VotesAccess(-1) + CheckUser.apply)
    )),
    ("tags", N < SchemaTags < (EndRelationRead(SchemaTags, TagLike)))
  )
}
