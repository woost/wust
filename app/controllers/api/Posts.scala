package controllers.api

import controllers.api.nodes.Nodes
import model.WustSchema._
import modules.db.access._
import modules.db.access.custom._
import modules.requests.dsl._

object Posts extends Nodes[Post] {
  val node = N(Post, PostAccess.apply,
    ("connects-from", N <--- (Connects, EndContentRelationAccess(Connects, Connectable),
      //TODO: should use HR --> to inject hyperrelationfactory...
      //("connects-to", HR --> StartContentRelationHyperAccess(Connects, Post)),
      ("connects-to", N --> new StartContentRelationHyperAccess(Connects, Post, Post, Connects, Post)),
      ("connects-from", N <-- new EndContentRelationHyperAccess(Connects, Post, Post, Connects, Post))
    )),
    ("connects-to", N ---> (Connects, StartContentRelationAccess(Connects, Post),
      ("connects-to", N --> new StartContentRelationHyperAccess(Connects, Post, Post, Connects, Post)),
      ("connects-from", N <-- new EndContentRelationHyperAccess(Connects, Post, Post, Connects, Post))
    )),
    ("tags", N <--- (Categorizes, EndRelationRead(Categorizes, Tag),
      ("voters", N <-- EndRelationRead(Votes, User)),
      ("up", N <-- VotesAccess(1)),
      ("down", N <-- VotesAccess(-1))
    ))
  )
}
