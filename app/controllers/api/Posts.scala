package controllers.api

import controllers.api.nodes.Nodes
import model.WustSchema.{Tags => SchemaTags, _}
import model.custom._
import modules.db.access._
import modules.db.access.custom._
import modules.requests.dsl._

object Posts extends Nodes[Post] {
  val node = NodeDef(Post, PostAccess.apply + TaggedTaggable.apply[Post],
    ("changes", N < EndRelationRead(UpdatedToContentNodeFactory, Updated)),
    ("votes", N < Dimensionizes < (EndRelationRead(Dimensionizes, VoteDimension),
      ("up", N < VotesAccess(1) + CheckUser.apply),
      ("down", N < VotesAccess(-1) + CheckUser.apply),
      ("neutral", N < VotesAccess(0) + CheckUser.apply)
    ))
  )
}
