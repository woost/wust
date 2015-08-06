package modules.db

import common.ConfigString._

import model.WustSchema._
import renesca.graph.Graph
import modules.db.GraphHelper._
import modules.db.types._
import play.api.Play.current
import renesca._
import renesca.parameter.implicits._
import renesca.schema._

object Database {
  val db = new DbService
  db.restService = new RestService(
    server = "db.neo4j.url".configOrElse("http://localhost:7474"),
    credentials = Some(spray.http.BasicHttpCredentials("db.neo4j.user".configOrElse("neo4j"), "db.neo4j.pass".configOrElse("neo4j")))
  )

  private def discourseGraphWithReturn(returns: String, definitions: GraphDefinition*): Discourse = {
    if(definitions.isEmpty || returns.isEmpty)
      return Discourse.empty

    val matcher = definitions.map(_.toQuery).mkString(",")
    val query = s"match $matcher return $returns"
    val params = definitions.map(_.parameterMap).reduce(_ ++ _)
    Discourse(db.queryGraph(Query(query, params)))
  }

  def discourseGraph(definitions: GraphDefinition*): Discourse = {
    discourseGraphWithReturn("*", definitions: _*)
  }

  def itemDiscourseGraph[NODE <: Node](definitions: GraphDefinition*): Discourse = {
    val returns = definitions.map(_.name).mkString(",")
    discourseGraphWithReturn(returns, definitions: _*)
  }

  def nodeDiscourseGraph[NODE <: UuidNode](factory: NodeFactory[NODE], uuids: String*): Discourse = {
    if(uuids.isEmpty)
      return discourseGraph(ConcreteFactoryNodeDefinition(factory))

    val query = s"match (n :`${ factory.label }`) where n.uuid in {uuids} return n limit ${ uuids.size }"
    val params = Map("uuids" -> uuids)
    Discourse(db.queryGraph(Query(query, params)))
  }

  def limitedDiscourseNodes[NODE <: UuidNode](skip: Int, limit: Int, factory: NodeFactory[NODE]): (Discourse, Seq[NODE]) = {
    val nodeDef = ConcreteFactoryNodeDefinition(factory)
    val discourse = discourseGraphWithReturn(s"${nodeDef.name} skip $skip limit $limit", nodeDef)
    (discourse, nodesWithType[NODE](discourse.nodes))
  }

  def discourseNodes[NODE <: UuidNode](factory: NodeFactory[NODE], uuids: String*): (Discourse, Seq[NODE]) = {
    val discourse = nodeDiscourseGraph(factory, uuids: _*)
    (discourse, findNodes(discourse, factory, uuids: _*))
  }

  def discourseNodes[START <: UuidNode, END <: UuidNode](startDefinition: UuidNodeDefinition[START], endDefinition: UuidNodeDefinition[END]): (Discourse, (Option[START], Option[END])) = {
    val discourse = itemDiscourseGraph(startDefinition, endDefinition)
    (discourse, findNodes(discourse, startDefinition, endDefinition))
  }

  def discourseNodes[NODE <: UuidNode](definitions: UuidNodeDefinition[NODE]*): (Discourse, Seq[NODE]) = {
    val discourse = itemDiscourseGraph(definitions: _*)
    (discourse, findNodes(discourse, definitions: _*))
  }

  def startConnectedDiscourseGraph[START <: Node, RELATION <: AbstractRelation[START, END], END <: Node](relationDefinitions: NodeAndFixedRelationDefinition[START, RELATION, END]*): Discourse = {
    val queries = relationDefinitions.map { relationDefinition =>
      val query = s"match ${ relationDefinition.toQuery } return ${ relationDefinition.endDefinition.name }"
      val params = relationDefinition.parameterMap
      Query(query, params)
    }
    Discourse(db.queryGraphs(queries: _*).fold(Graph.empty)(_ merge _))
  }

  def startConnectedDiscourseNodes[START <: Node, RELATION <: AbstractRelation[START, END], END <: Node](relationDefinitions: NodeAndFixedRelationDefinition[START, RELATION, END]*): Seq[END] = {
    val discourse = startConnectedDiscourseGraph(relationDefinitions: _*)
    nodesWithType[END](discourse.nodes)
  }

  //TODO: need ordering for limit search, otherwise we get duplicates
  def limitedStartConnectedDiscourseGraph[START <: Node, RELATION <: AbstractRelation[START, END], END <: Node](skip: Int, limit: Int, relationDefinitions: NodeAndFixedRelationDefinition[START, RELATION, END]*): Discourse = {
    val queries = relationDefinitions.map { relationDefinition =>
      val query = s"match ${ relationDefinition.toQuery } return ${ relationDefinition.endDefinition.name } skip $skip limit $limit"
      val params = relationDefinition.parameterMap
      Query(query, params)
    }
    Discourse(db.queryGraphs(queries: _*).fold(Graph.empty)(_ merge _))
  }

  def limitedStartConnectedDiscourseNodes[START <: Node, RELATION <: AbstractRelation[START, END], END <: Node](skip: Int, limit: Int, relationDefinitions: NodeAndFixedRelationDefinition[START, RELATION, END]*): Seq[END] = {
    val discourse = limitedStartConnectedDiscourseGraph(skip, limit, relationDefinitions: _*)
    nodesWithType[END](discourse.nodes)
  }

  def endConnectedDiscourseGraph[START <: Node, RELATION <: AbstractRelation[START, END], END <: Node](relationDefinitions: FixedAndNodeRelationDefinition[START, RELATION, END]*): Discourse = {
    val queries = relationDefinitions.map { relationDefinition =>
      val query = s"match ${ relationDefinition.toQuery } return ${ relationDefinition.startDefinition.name }"
      val params = relationDefinition.parameterMap
      Query(query, params)
    }
    Discourse(db.queryGraphs(queries: _*).fold(Graph.empty)(_ merge _))
  }

  def endConnectedDiscourseNodes[START <: Node, RELATION <: AbstractRelation[START, END], END <: Node](relationDefinitions: FixedAndNodeRelationDefinition[START, RELATION, END]*): Seq[START] = {
    val discourse = endConnectedDiscourseGraph(relationDefinitions: _*)
    nodesWithType[START](discourse.nodes)
  }

  def limitedEndConnectedDiscourseGraph[START <: Node, RELATION <: AbstractRelation[START, END], END <: Node](skip: Int, limit: Int, relationDefinitions: FixedAndNodeRelationDefinition[START, RELATION, END]*): Discourse = {
    val queries = relationDefinitions.map { relationDefinition =>
      val query = s"match ${ relationDefinition.toQuery } return ${ relationDefinition.startDefinition.name } skip $skip limit $limit"
      val params = relationDefinition.parameterMap
      Query(query, params)
    }
    Discourse(db.queryGraphs(queries: _*).fold(Graph.empty)(_ merge _))
  }

  def limitedEndConnectedDiscourseNodes[START <: Node, RELATION <: AbstractRelation[START, END], END <: Node](skip: Int, limit: Int, relationDefinitions: FixedAndNodeRelationDefinition[START, RELATION, END]*): Seq[START] = {
    val discourse = limitedEndConnectedDiscourseGraph(skip, limit, relationDefinitions: _*)
    nodesWithType[START](discourse.nodes)
  }

  def deleteNodes[NODE <: UuidNode](definitions: NodeDefinition[NODE]*) {
    val discourse = discourseGraph(definitions: _*)
    discourse.graph.nodes.clear()
    db.transaction{ tx =>
      tx.persistChanges(discourse.graph)
      model.WustSchema.deleteConnectsGarbage(tx)
    }
  }

  //TODO: unique connections?
  def connectNodes[START <: Node, RELATION <: AbstractRelation[START, END], END <: Node](discourse: Discourse, start: START, factory: ContentRelationFactory[START, RELATION, END], end: END): (START, END) = {
    discourse.add(factory.createContentRelation(start, end))
    db.transaction(_.persistChanges(discourse.graph))
    (start, end)
  }

  //TODO: merge with previous
  def connectNodes(discourse: Discourse, start: User, factory: Votes.type, end: Categorizes, weight: Long): (User, Categorizes) = {
    discourse.add(factory.merge(start, end, weight = weight, onMatch = Set("weight")))
    db.transaction(_.persistChanges(discourse.graph))
    (start, end)
  }

  def connectUuidNodes[START <: UuidNode, RELATION <: AbstractRelation[START, END], END <: UuidNode](relationDefinition: UuidRelationDefinition[START, RELATION, END] with ContentRelationDefinition[START, RELATION, END]): Option[(START, END)] = {
    import relationDefinition._
    val (discourse, (startOpt, endOpt)) = discourseNodes(startDefinition, endDefinition)
    if(startOpt.isEmpty || endOpt.isEmpty)
      None
    else
      Some(connectNodes(discourse, startOpt.get, factory, endOpt.get))
  }

  def gatherHyperNodesConnector[BASE <: Node, OTHER <: UuidNode](baseDef: HyperNodeDefinitionBase[BASE], otherDef: UuidNodeDefinition[OTHER]): (Discourse, Option[(BASE, OTHER)]) = {
    val discourse = itemDiscourseGraph(baseDef, otherDef)
    val nodeOpt = nodeWithUuid[OTHER](discourse, otherDef.uuid)
    if(nodeOpt.isEmpty)
      return (discourse, None)

    val node = nodeOpt.get
    val middleOpt = discourse.hyperRelations.filterNot(_ == node).headOption
    if(middleOpt.isEmpty)
      return (discourse, None)

    val middle = middleOpt.get.asInstanceOf[BASE]
    (discourse, Some((middle, node)))
  }

  def startConnectHyperNodes[START <: Node, RELATION <: AbstractRelation[START, END], END <: UuidNode](relationDefinition: HyperAndNodeRelationDefinition[START, RELATION, END] with NodeAndUuidRelationDefinition[START, RELATION, END] with ContentRelationDefinition[START, RELATION, END]): Option[(START, END)] = {
    import relationDefinition._
    val (discourse, nodesOpt) = gatherHyperNodesConnector(startDefinition, endDefinition)
    if(nodesOpt.isEmpty)
      None
    else
      Some(connectNodes(discourse, nodesOpt.get._1, factory, nodesOpt.get._2))
  }

  def endConnectHyperNodes[START <: UuidNode, RELATION <: AbstractRelation[START, END], END <: Node](relationDefinition: NodeAndHyperRelationDefinition[START, RELATION, END] with UuidAndNodeRelationDefinition[START, RELATION, END] with ContentRelationDefinition[START, RELATION, END]): Option[(START, END)] = {
    import relationDefinition._
    val (discourse, nodesOpt) = gatherHyperNodesConnector(endDefinition, startDefinition)
    if(nodesOpt.isEmpty)
      None
    else
      Some(connectNodes(discourse, nodesOpt.get._2, factory, nodesOpt.get._1))
  }

  def disconnectNodes[START <: Node, RELATION <: AbstractRelation[START, END], END <: Node](relationDefinition: FixedRelationDefinition[START, RELATION, END]) {
    val tx = db.newTransaction()
    val discourse = itemDiscourseGraph(relationDefinition)
    if(discourse.relations.isEmpty && discourse.hyperRelations.size == 1) {
      val label = discourse.hyperRelations.head.label
      discourse.graph.nodes -= discourse.hyperRelations.head.rawItem
      tx.persistChanges(discourse.graph)
      // here we disconnect a hyperrelation
      // therefore we garbage collect broken hyperrelations
      // in our case, only CONNECTS is recursive
      // TODO: challenge, this is a workaround
      if(label == Connects.label)
        model.WustSchema.deleteConnectsGarbage(tx)
      tx.commit()
    }
    else {
      discourse.graph.relations.clear()
      tx.commit.persistChanges(discourse.graph)
    }
  }

  def connectedComponent(focusNode: UuidNodeDefinition[_], depth: Int = 5): Discourse = {
    // depth * 2 because hyperrelation depth
    val query = s"""
      match ${ focusNode.toQuery }
      match (${ focusNode.name })-[rel:`${ Connects.startRelationType }`|`${ Connects.endRelationType }` *0..${ depth * 2 }]-(all:POST)
      optional match (tag:TAGLIKE)-[categorizesRel1:`${ Categorizes.startRelationType }`]->(:`${ Categorizes.label }`)-[categorizesRel2:`${ Categorizes.endRelationType }`]->(all)
      return distinct all,rel,tag,categorizesRel1,categorizesRel2
    """
    val params = focusNode.parameterMap
    Discourse(db.queryGraph(Query(query, params)))
  }
}
