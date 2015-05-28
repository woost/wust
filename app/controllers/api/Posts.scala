package controllers.api

import controllers.api.nodes.Nodes
import model.WustSchema._
import modules.db.access.{ContentNodeAccess, EndContentRelationAccess, StartContentRelationAccess}
import modules.requests._
import renesca.schema.{AbstractRelationFactory, NodeFactory, AbstractRelation}

object Posts extends Nodes[Post] {
  lazy val nodeSchema = NodeSchema(routePath, new ContentNodeAccess(Post),
    Map(
      "connectsTo" -> StartHyperConnectSchema(Connects, new StartContentRelationAccess(Connects, Post), Map(
        "connectsTo" -> StartConnectSchema(new StartContentRelationAccess(Connects, Post)),
        "connectsFrom" -> EndConnectSchema(new EndContentRelationAccess(Connects, Post))
      )),
      "connectsFrom" -> EndHyperConnectSchema(Connects, new EndContentRelationAccess(Connects, Post), Map(
        "connectsTo" -> StartConnectSchema(new StartContentRelationAccess(Connects, Post)),
        "connectsFrom" -> EndConnectSchema(new EndContentRelationAccess(Connects, Post))
      ))
    )
  )
}
