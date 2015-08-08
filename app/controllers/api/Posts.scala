package controllers.api

import controllers.api.nodes.Nodes
import model.WustSchema._
import modules.db.access._
import modules.db.access.custom._
import modules.requests.dsl._

object Posts extends Nodes[Post] {
  val node = NodeDef(Post, PostAccess.apply,
    ("connects-from", N < Connects < (EndContentRelationAccess(Connects, Post) + PostAccess.apply,
      ("connects-to", N > StartContentRelationAccess(Connects, Post) + PostAccess.apply),
      ("connects-from", N < EndContentRelationAccess(Connects, Post) + PostAccess.apply)
    )),
    ("connects-to", N > Connects > (StartContentRelationAccess(Connects, Post) + PostAccess.apply,
      ("connects-to", N > StartContentRelationAccess(Connects, Post) + PostAccess.apply),
      ("connects-from", N < EndContentRelationAccess(Connects, Post) + PostAccess.apply)
    )),
    ("tags", N < Categorizes < (EndRelationRead(Categorizes, Tag),
      ("voters", N < EndRelationRead(Votes, User)),
      ("up", N < VotesAccess(1) + CheckUser.apply),
      ("down", N < VotesAccess(-1) + CheckUser.apply)
    ))
  )
}
