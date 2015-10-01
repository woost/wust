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
      val tagDef = ConcreteFactoryNodeDefinition(Scope)
      val nodeDef = ConcreteFactoryNodeDefinition(Post)
      val connectsDef = ConcreteFactoryNodeDefinition(Connects)
      val tagsDef = HyperNodeDefinition(tagDef, Tags, nodeDef)
      val tagClassifiesDef = RelationDefinition(ConcreteFactoryNodeDefinition(Classification), Classifies, tagsDef)
      val connDef = RelationDefinition(nodeDef, ConnectsStart, connectsDef)
      val classifiesDef = RelationDefinition(ConcreteFactoryNodeDefinition(Classification), Classifies, connectsDef)

      val query = s"""
      match ${ nodeDef.toQuery } where ${ nodeDef.name }.uuid in {nodeUuids}
      optional match ${ tagsDef.toQuery(true, false) }
      optional match ${ tagClassifiesDef.toQuery(true, false) }
      optional match ${ connDef.toQuery(false, true) }, ${ classifiesDef.toQuery(true, false) }
      return *
      """

      val params = tagsDef.parameterMap ++ connDef.parameterMap ++ tagClassifiesDef.parameterMap ++ classifiesDef.parameterMap ++ Map("nodeUuids" -> response.map(_.uuid).toSeq)

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
      val classDef = ConcreteFactoryNodeDefinition(Classification)
      val nodeDef = ConcreteFactoryNodeDefinition(Reference)
      val relDef = RelationDefinition(classDef, Classifies, nodeDef)

      val query = s"""
      match ${ relDef.toQuery }
      where ${ nodeDef.name }.uuid in {nodeUuids}
      return *
      """
      val params = nodeDef.parameterMap ++ classDef.parameterMap ++ Map("nodeUuids" -> response.map(_.uuid).toSeq)

      val graph = db.queryGraph(Query(query, params.toMap))
      val discourse = Discourse(response.head.graph merge graph)
      discourse.add(response.toSeq: _*)
    }

    response
  }
}
