package modules.db

import common.ConfigString._
import model.WustSchema._
import play.api.Play.current
import renesca._
import renesca.graph.Label
import renesca.parameter.implicits._
import renesca.schema._

object Database {
  val db = new DbService
  db.restService = new RestService(
    server = "db.neo4j.url".configOrElse("http://localhost:7474"),
    credentials = Some(spray.http.BasicHttpCredentials("db.neo4j.user".configOrElse("neo4j"), "db.neo4j.pass".configOrElse("neo4j")))
  )

  private def nodeWithUuid[NODE <: UuidNode](discourse: Discourse, uuid: String) = discourse.uuidNodes.find(_.uuid == uuid).get.asInstanceOf[NODE]
  private def nodesWithType[NODE <: Node](nodes: Set[Node]) = nodes.map(_.asInstanceOf[NODE])

  def wholeDiscourseGraph: Discourse = Discourse(db.queryGraph("match (n) optional match (n)-[r]-() return n,r"))

  def labelDiscourseGraph(label: Label): Discourse = Discourse(db.queryGraph(s"match (n :`$label`) return n"))

  def discourseGraph(definitions: GraphDefinition*): Discourse = {
    if(definitions.isEmpty)
      return Discourse.empty

    val matcher = definitions.map(_.toQuery).mkString(",")
    val query = s"match $matcher return *"
    val params = definitions.map(_.parameterMap).reduce(_ ++ _)
    Discourse(db.queryGraph(Query(query, params)))
  }

  def nodeDiscourseGraph[START <: Node, END <: Node](definitions: NodeDefinition[START]*): Discourse = {
    if(definitions.isEmpty)
      return Discourse.empty

    val matcher = definitions.map(_.toQuery).mkString(",")
    val returns = definitions.map(_.name).mkString(",")
    val query = s"match $matcher return $returns"
    val params = definitions.map(_.parameterMap).reduce(_ ++ _)
    Discourse(db.queryGraph(Query(query, params)))
  }

  def nodeDiscourseGraph[NODE <: UuidNode](factory: NodeFactory[NODE], uuids: String*): Discourse = {
    if(uuids.isEmpty)
      return labelDiscourseGraph(factory.label)

    val query = s"match (n :`${ factory.label }`) where n.uuid in {uuids} return n limit ${ uuids.size }"
    val params = Map("uuids" -> uuids)
    Discourse(db.queryGraph(Query(query, params)))
  }

  def discourseNodes[NODE <: UuidNode](factory: NodeFactory[NODE], uuids: String*): (Discourse, Seq[NODE]) = {
    val discourse = nodeDiscourseGraph(factory, uuids: _*)
    if(uuids.isEmpty)
      (discourse, nodesWithType[NODE](discourse.nodes).toSeq)
    else
      (discourse, uuids.map { uuid => nodeWithUuid[NODE](discourse, uuid) })
  }

  def discourseNodes[START <: UuidNode, END <: UuidNode](startDefinition: UuidNodeDefinition[START], endDefinition: UuidNodeDefinition[END]): (Discourse, (START, END)) = {
    val discourse = nodeDiscourseGraph(startDefinition, endDefinition)
    (discourse, (nodeWithUuid[START](discourse, startDefinition.uuid), nodeWithUuid[END](discourse, endDefinition.uuid)))
  }

  def startConnectedDiscourseGraph[START <: Node, RELATION <: AbstractRelation[START, END], END <: Node](relationDefinition: StartFixedNodeRelationDefinition[START, RELATION, END]): Discourse = {
    val query = s"match ${ relationDefinition.toQuery } return ${ relationDefinition.endDefinition.name }"
    val params = relationDefinition.parameterMap
    Discourse(db.queryGraph(Query(query, params)))
  }

  def startConnectedDiscourseNodes[START <: Node, RELATION <: AbstractRelation[START, END], END <: Node](relationDefinition: StartFixedNodeRelationDefinition[START, RELATION, END]): (Discourse, Set[END]) = {
    val discourse = startConnectedDiscourseGraph(relationDefinition)
    (discourse, nodesWithType[END](discourse.nodes))
  }

  def endConnectedDiscourseGraph[START <: Node, RELATION <: AbstractRelation[START, END], END <: Node](relationDefinition: EndFixedNodeRelationDefinition[START, RELATION, END]): Discourse = {
    val query = s"match ${ relationDefinition.toQuery } return ${ relationDefinition.startDefinition.name }"
    val params = relationDefinition.parameterMap
    Discourse(db.queryGraph(Query(query, params)))
  }

  def endConnectedDiscourseNodes[START <: Node, RELATION <: AbstractRelation[START, END], END <: Node](relationDefinition: EndFixedNodeRelationDefinition[START, RELATION, END]): (Discourse, Set[START]) = {
    val discourse = endConnectedDiscourseGraph(relationDefinition)
    (discourse, nodesWithType[START](discourse.nodes))
  }

  def deleteNodes[NODE <: UuidNode](definitions: NodeDefinition[NODE]*) {
    val discourse = discourseGraph(definitions: _*)
    discourse.graph.nodes.clear()
    db.persistChanges(discourse.graph)
  }

  def connectNodes[START <: Node, RELATION <: AbstractRelation[START, END], END <: Node](discourse: Discourse, start: START, factory: ContentRelationFactory[START, RELATION, END], end: END): (START, END) = {
    discourse.add(factory.local(start, end))
    db.persistChanges(discourse.graph)
    (start, end)
  }

  def connectUuidNodes[START <: UuidNode, RELATION <: AbstractRelation[START, END], END <: UuidNode](relationDefinition: UuidNodeRelationDefinition[START,RELATION,END] with ContentRelationDefinition[START,RELATION,END]): (START,END) = { import relationDefinition._
    val (discourse, (start,end)) = discourseNodes(startDefinition, endDefinition)
    connectNodes(discourse, start, factory, end)
  }

  def startConnectHyperNodes[START <: Node, RELATION <: AbstractRelation[START, END], END <: UuidNode](relationDefinition: FixedUuidNodeRelationDefinition[START,RELATION,END] with ContentRelationDefinition[START,RELATION,END]): (START,END) = { import relationDefinition._
    val discourse = nodeDiscourseGraph(startDefinition, endDefinition)
    val node = nodeWithUuid[END](discourse, endDefinition.uuid)
    val middle = discourse.uuidNodeHyperRelations.filterNot(_ != node).head.asInstanceOf[START]
    connectNodes(discourse, middle, factory, node)
  }

  def endConnectHyperNodes[START <: UuidNode, RELATION <: AbstractRelation[START, END], END <: Node](relationDefinition: UuidHyperNodeRelationDefinition[START,RELATION,END] with ContentRelationDefinition[START,RELATION,END]): (START,END) = { import relationDefinition._
    val discourse = nodeDiscourseGraph(startDefinition, endDefinition)
    val node = nodeWithUuid[START](discourse, startDefinition.uuid)
    val middle = discourse.uuidNodeHyperRelations.filterNot(_ != node).head.asInstanceOf[END]
    connectNodes(discourse, node, factory, middle)
  }

  def disconnectNodes[START <: Node, RELATION <: AbstractRelation[START, END], END <: Node](relationDefinition: NodeRelationDefinition[START,RELATION,END]) {
    val query = s"match ${relationDefinition.toQuery} delete ${relationDefinition.name}"
    val params = relationDefinition.parameterMap
    db.query(Query(query, params))
  }
}
