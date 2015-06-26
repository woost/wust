package controllers.api

import controllers.api.nodes.Nodes
import model.WustSchema._
import modules.db.access._
import modules.requests._
import renesca.schema.{AbstractRelationFactory, NodeFactory, AbstractRelation}

object Posts extends Nodes[Post] {
  lazy val nodeSchema = NodeSchema(routePath, new PostAccess,
    Map(
      "connects-to" -> StartHyperConnectSchema(Connects, new StartContentRelationAccess(Connects, Post), Map(
        "connects-to" -> StartConnectSchema(new StartContentRelationAccess(Connects, Post)),
        "connects-from" -> EndConnectSchema(new EndContentRelationAccess(Connects, Post))
      )),
      "connects-from" -> EndHyperConnectSchema(Connects, new EndContentRelationAccess(Connects, Post), Map(
        "connects-to" -> StartConnectSchema(new StartContentRelationAccess(Connects, Post)),
        "connects-from" -> EndConnectSchema(new EndContentRelationAccess(Connects, Post))
      )),
      "tags" -> EndHyperConnectSchema(Categorizes, new EndRelationRead(Categorizes, Tag), Map(
      //TODO: this inconsistent: creation only works for up/down votes separately
      // whereas reading returns any type of vote on both paths up/down
        "up" -> EndConnectSchema(new VotesAccess(Votes, 1)),
        "down" -> EndConnectSchema(new VotesAccess(Votes, -1))
      ))
    )
  )
}
