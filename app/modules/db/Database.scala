package modules.db

import common.ConfigString._
import model.WustSchema._
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
      return discourseGraph(LabelNodeDefinition(factory))

    val query = s"match (n :`${ factory.label }`) where n.uuid in {uuids} return n limit ${ uuids.size }"
    val params = Map("uuids" -> uuids)
    Discourse(db.queryGraph(Query(query, params)))
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

  def startConnectedDiscourseGraph[START <: Node, RELATION <: AbstractRelation[START, END], END <: Node](relationDefinition: NodeAndFixedRelationDefinition[START, RELATION, END]): Discourse = {
    val query = s"match ${ relationDefinition.toQuery } return ${ relationDefinition.endDefinition.name }"
    val params = relationDefinition.parameterMap
    Discourse(db.queryGraph(Query(query, params)))
  }

  def startConnectedDiscourseNodes[START <: Node, RELATION <: AbstractRelation[START, END], END <: Node](relationDefinition: NodeAndFixedRelationDefinition[START, RELATION, END]): Set[END] = {
    val discourse = startConnectedDiscourseGraph(relationDefinition)
    nodesWithType[END](discourse.nodes)
  }

  def endConnectedDiscourseGraph[START <: Node, RELATION <: AbstractRelation[START, END], END <: Node](relationDefinition: FixedAndNodeRelationDefinition[START, RELATION, END]): Discourse = {
    val query = s"match ${ relationDefinition.toQuery } return ${ relationDefinition.startDefinition.name }"
    val params = relationDefinition.parameterMap
    Discourse(db.queryGraph(Query(query, params)))
  }

  def endConnectedDiscourseNodes[START <: Node, RELATION <: AbstractRelation[START, END], END <: Node](relationDefinition: FixedAndNodeRelationDefinition[START, RELATION, END]): Set[START] = {
    val discourse = endConnectedDiscourseGraph(relationDefinition)
    nodesWithType[START](discourse.nodes)
  }

  // TODO: leaks hyperedges
  def deleteNodes[NODE <: UuidNode](definitions: NodeDefinition[NODE]*) {
    val discourse = discourseGraph(definitions: _*)
    discourse.graph.nodes.clear()
    db.persistChanges(discourse.graph)
  }

  //TODO: unique connections?
  def connectNodes[START <: Node, RELATION <: AbstractRelation[START, END], END <: Node](discourse: Discourse, start: START, factory: ContentRelationFactory[START, RELATION, END], end: END): (START, END) = {
    discourse.add(factory.localContentRelation(start, end))
    db.persistChanges(discourse.graph)
    (start, end)
  }

  def connectUuidNodes[START <: UuidNode, RELATION <: AbstractRelation[START, END], END <: UuidNode](relationDefinition: UuidRelationDefinition[START,RELATION,END] with ContentRelationDefinition[START,RELATION,END]): Option[(START,END)] = { import relationDefinition._
    val (discourse, (startOpt,endOpt)) = discourseNodes(startDefinition, endDefinition)
    if (startOpt.isEmpty || endOpt.isEmpty)
      None
    else
      Some(connectNodes(discourse, startOpt.get, factory, endOpt.get))
  }

  def gatherHyperNodesConnector[BASE <: Node, OTHER <: UuidNode](baseDef: HyperNodeDefinitionBase[BASE], otherDef: UuidNodeDefinition[OTHER]): (Discourse, Option[(BASE,OTHER)]) = {
    val discourse = itemDiscourseGraph(baseDef, otherDef)
    val nodeOpt = nodeWithUuid[OTHER](discourse, otherDef.uuid)
    if (nodeOpt.isEmpty)
      return (discourse, None)

    val node = nodeOpt.get
    val middleOpt = discourse.hyperRelations.filterNot(_ == node).headOption
    if (middleOpt.isEmpty)
      return (discourse, None)

    val middle = middleOpt.get.asInstanceOf[BASE]
    (discourse, Some((middle,node)))
  }

  def startConnectHyperNodes[START <: Node, RELATION <: AbstractRelation[START, END], END <: UuidNode](relationDefinition: HyperAndNodeRelationDefinition[START,RELATION,END] with NodeAndUuidRelationDefinition[START,RELATION,END] with ContentRelationDefinition[START,RELATION,END]): Option[(START,END)] = { import relationDefinition._
    val (discourse, nodesOpt) = gatherHyperNodesConnector(startDefinition, endDefinition)
    if (nodesOpt.isEmpty)
      None
    else
      Some(connectNodes(discourse, nodesOpt.get._1, factory, nodesOpt.get._2))
  }

  def endConnectHyperNodes[START <: UuidNode, RELATION <: AbstractRelation[START, END], END <: Node](relationDefinition: NodeAndHyperRelationDefinition[START,RELATION,END] with UuidAndNodeRelationDefinition[START,RELATION,END] with ContentRelationDefinition[START,RELATION,END]): Option[(START,END)] = { import relationDefinition._
    val (discourse, nodesOpt) = gatherHyperNodesConnector(endDefinition, startDefinition)
    if (nodesOpt.isEmpty)
      None
    else
      Some(connectNodes(discourse, nodesOpt.get._2, factory, nodesOpt.get._1))
  }

  def disconnectNodes[START <: Node, RELATION <: AbstractRelation[START, END], END <: Node](relationDefinition: FixedRelationDefinition[START,RELATION,END]) {
    val discourse = itemDiscourseGraph(relationDefinition)
    if (discourse.relations.isEmpty && discourse.hyperRelations.size == 1)
      discourse.graph.nodes -= discourse.hyperRelations.head.node
    else
      discourse.graph.relations.clear()
    db.persistChanges(discourse.graph)
  }

  def connectedComponent(focusNode: UuidNodeDefinition[_], depth:Int = 5): Discourse = {
    val query = s"""
      match ${focusNode.toQuery}
      match (${focusNode.name})-[rel:POSTLIKETOCONNECTS|CONNECTSTOPOSTLIKE *0..${depth * 2}]-(all:POST)
      optional match (tag:TAG)-[categorizesRel1:TAGTOCATEGORIZESPOST]->(:CATEGORIZESPOST)-[categorizesRel2:CATEGORIZESPOSTTOPOST]->(all)
      return distinct all,rel,tag,categorizesRel1,categorizesRel2
    """
    val params = focusNode.parameterMap
    Discourse(db.queryGraph(Query(query, params)))
  }
}
