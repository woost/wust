package controllers.api

import controllers.api.nodes.Nodes
import model.WustSchema._
import modules.db.access._
import modules.db.access.custom.{VotesAccess, EndContentRelationAccess, StartContentRelationAccess, PostAccess}
import modules.requests.dsl._

object Posts extends Nodes[Post] {
  val node = N(Post, PostAccess.apply,
    ("connects-from", N <--- (Connects, EndContentRelationAccess(Connects, Post),
      ("connects-to", N --> StartContentRelationAccess(Connects, Post)),
      ("connects-from", N <-- EndContentRelationAccess(Connects, Post))
    )),
    ("connects-to", N ---> (Connects, StartContentRelationAccess(Connects, Post),
      ("connects-to", N --> StartContentRelationAccess(Connects, Post)),
      ("connects-from", N <-- EndContentRelationAccess(Connects, Post))
    )),
    ("tags", N <--- (Categorizes, EndRelationRead(Categorizes, Tag),
      ("voters", N <-- EndRelationRead(Votes, User)),
      ("up", N <-- VotesAccess(1)),
      ("down", N <-- VotesAccess(-1))
    ))
  )
}
