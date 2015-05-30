package controllers.api

import controllers.api.nodes.Nodes
import model.WustSchema._
import modules.db.access.{ContentNodeAccess, EndContentRelationAccess, StartContentRelationAccess}
import modules.requests._
import renesca.schema.{AbstractRelationFactory, NodeFactory, AbstractRelation}

object Posts extends Nodes[Post] {
  lazy val nodeSchema = NodeSchema(routePath, new ContentNodeAccess(Post),
    Map(
      "connects-to" -> StartHyperConnectSchema(Connects, new StartContentRelationAccess(Connects, Post), Map(
        "connects-to" -> StartConnectSchema(new StartContentRelationAccess(Connects, Post)),
        "connects-from" -> EndConnectSchema(new EndContentRelationAccess(Connects, Post))
      )),
      "connects-from" -> EndHyperConnectSchema(Connects, new EndContentRelationAccess(Connects, Post), Map(
        "connects-to" -> StartConnectSchema(new StartContentRelationAccess(Connects, Post)),
        "connects-from" -> EndConnectSchema(new EndContentRelationAccess(Connects, Post))
      ))
    )
  )
}
