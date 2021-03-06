package modules.db.helpers

import model.WustSchema._
import modules.db.Database._
import modules.db._
import renesca.Query
import renesca.parameter.implicits._

// adds tags to the results by querying for them and adding them to the graph
// it might be better to implement this in the nodeaccess/relationaccess directly
// because shapeResponse takes the original response and queries for its tags.
// so this costs an extra query. instead we could implement this in a node- or
// relationaccess and include the tags in the read queries.
object TaggedTaggable {
  def shapeResponse[NODE <: UuidNode](response: NODE): NODE = {
    shapeResponse(List(response)).head
  }

  def shapeResponse[NODE <: UuidNode](response: Iterable[NODE]): Iterable[NODE] = {
    if(!response.isEmpty) {
      implicit val ctx = new QueryContext
      val tagDef = FactoryNodeDef(Scope)
      val nodeDef = FactoryNodeDef(Post)
      val connectsDef = FactoryNodeDef(Connects)
      val tagsDef = HyperNodeDef(tagDef, Tags, nodeDef)
      val tagClassifiesDef = RelationDef(FactoryNodeDef(Classification), Classifies, tagsDef)
      val connDef = RelationDef(nodeDef, ConnectsStart, connectsDef)
      val classifiesDef = RelationDef(FactoryNodeDef(Classification), Classifies, connectsDef)

      val query = s"""
      match ${ nodeDef.toPattern } where ${ nodeDef.name }.uuid in {nodeUuids}
      optional match ${ tagsDef.toPattern(true, false) }
      optional match ${ tagClassifiesDef.toPattern(true, false) }
      optional match ${ connDef.toPattern(false, true) }, ${ classifiesDef.toPattern(true, false) }
      return *
      """

      val params = ctx.params ++ Map("nodeUuids" -> response.map(_.uuid).toSeq)

      val graph = db.queryGraph(Query(query, params.toMap))
      val discourse = Discourse(response.head.graph merge graph)
      discourse.add(response.toSeq: _*)
    }

    response
  }
}

object ClassifiedReferences {
  def shapeResponse[NODE <: UuidNode](response: NODE): NODE = {
    shapeResponse(List(response)).head
  }

  def shapeResponse[NODE <: UuidNode](response: Iterable[NODE]): Iterable[NODE] = {
    if(!response.isEmpty) {
      implicit val ctx = new QueryContext
      val classDef = FactoryNodeDef(Classification)
      val nodeDef = FactoryNodeDef(Reference)
      val relDef = RelationDef(classDef, Classifies, nodeDef)

      val query = s"""
      match ${ relDef.toPattern }
      where ${ nodeDef.name }.uuid in {nodeUuids}
      return *
      """
      val params = ctx.params ++ Map("nodeUuids" -> response.map(_.uuid).toSeq)
      val graph = db.queryGraph(Query(query, params))
      val discourse = Discourse(response.head.graph merge graph)
      discourse.add(response.toSeq: _*)
    }

    response
  }
}
