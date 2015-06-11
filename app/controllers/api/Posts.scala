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
      "tags" -> EndHyperConnectSchema(CategorizesPost, new EndRelationRead(CategorizesPost, Tag), Map(
        "up" -> EndConnectSchema(new VotesAccess(UpVotes)),
        "down" -> EndConnectSchema(new VotesAccess(DownVotes))
      ))
    )
  )
}
