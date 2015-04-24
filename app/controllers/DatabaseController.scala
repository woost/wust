package controllers

import model.WustSchema._
import model._
import renesca._
import renesca.schema._
import renesca.graph.RelationType
import renesca.parameter.ParameterMap
import renesca.parameter.implicits._
import common.ConfigString._
import play.api.Play.current
import renesca.graph.Label

trait DatabaseController {
  val db = new DbService
  db.restService = new RestService(
    server = "db.neo4j.url".configOrElse("http://localhost:7474"),
      credentials = Some(spray.http.BasicHttpCredentials("db.neo4j.user".configOrElse("neo4j"),"db.neo4j.pass".configOrElse("neo4j")))
    )

  private def nodeWithUuid[NODE <: UuidNode](discourse: Discourse, uuid: String) = discourse.uuidNodes.find(_.uuid == uuid).get.asInstanceOf[NODE]

  private def relationMatcherQuery[RELATION <: SchemaAbstractRelation[_,_] with SchemaItem](relFactory: SchemaAbstractRelationFactory[_,RELATION,_]) = {
    relFactory match {
      case r: SchemaRelationFactory[_,RELATION,_] => s"[relation :`${r.relationType}`]"
      case r: SchemaHyperRelationFactory[_,_,RELATION,_,_] => s"[startRelation :`${r.startRelationType}`]->(middle :`${r.label}`)-[endRelation :`${r.endRelationType}`]"
    }
  }

  def wholeDiscourseGraph: Discourse = Discourse(db.queryGraph("match (n) optional match (n)-[r]-() return n,r"))

  def labelDiscourseGraph(label: Label): Discourse = Discourse(db.queryGraph(s"match (n :`$label`) return n"))

  def nodeDiscourseGraph[START <: UuidNode, END <: UuidNode](startFactory: SchemaNodeFactory[START], startUuid: String, endFactory: SchemaNodeFactory[END], endUuid: String): Discourse = {
    val query = s"match (start :`${startFactory.label}` {uuid: {startUuid}}), (end: `${endFactory.label}` {uuid: {endUuid}}) return start,end limit 2"
    val params = Map("startUuid" -> startUuid, "endUuid" -> endUuid)

    Discourse(db.queryGraph(Query(query, params)))
  }

  def nodeDiscourseGraph[NODE <: UuidNode](factory: SchemaNodeFactory[NODE], uuids: String*): Discourse = {
    if (uuids.isEmpty)
      return labelDiscourseGraph(factory.label)

    val query = s"match (n :`${factory.label}`) where n.uuid in {uuids} return n limit ${uuids.size}"
    val params = Map("uuids" -> uuids)

    Discourse(db.queryGraph(Query(query, params)))
  }


  def discourseNodes[NODE <: UuidNode](factory: SchemaNodeFactory[NODE], uuids: String*) = {
    val discourse = nodeDiscourseGraph(factory, uuids:_*)
    if (uuids.isEmpty)
      (discourse, discourse.uuidNodes.map(_.asInstanceOf[NODE]))
    else
      (discourse, uuids.map {uuid => nodeWithUuid[NODE](discourse, uuid)})
  }

  def discourseNodes[START <: UuidNode, END <: UuidNode](startFactory: SchemaNodeFactory[START], startUuid: String, endFactory: SchemaNodeFactory[END], endUuid: String) = {
    val discourse = nodeDiscourseGraph(startFactory, startUuid, endFactory, endUuid)
    (discourse, (nodeWithUuid[START](discourse, startUuid), nodeWithUuid[END](discourse, endUuid)))
  }

  def relationDiscourseGraph[START <: UuidNode, RELATION <: SchemaAbstractRelation[START,END] with SchemaItem, END <: UuidNode](startUuid: String, relFactory: SchemaAbstractRelationFactory[START,RELATION,END], endUuid: String): Discourse = {
    val query = s"match (start :`${relFactory.startNodeFactory.label}` {uuid: {startUuid}})-${relationMatcherQuery(relFactory)}->(end :`${relFactory.endNodeFactory.label}` {uuid: {endUuid}}) return *"
    val params = Map("startUuid" -> startUuid, "endUuid" -> endUuid)

    Discourse(db.queryGraph(Query(query, params)))
  }

  def connectedNodesDiscourseGraph[START <: UuidNode, RELATION <: SchemaAbstractRelation[START,_] with SchemaItem](startUuid: String, relFactory: SchemaAbstractRelationFactory[START,RELATION,_]): Discourse = {
    val query = s"match (start :`${relFactory.startNodeFactory.label}` {uuid: {startUuid}})-${relationMatcherQuery(relFactory)}->(end :`${relFactory.endNodeFactory.label}`) return end"
    val params = Map("startUuid" -> startUuid)

    Discourse(db.queryGraph(Query(query, params)))
  }

  def connectedNodesDiscourseGraph[RELATION <: SchemaAbstractRelation[_,END] with SchemaItem, END <: UuidNode](relFactory: SchemaAbstractRelationFactory[_,RELATION,END], endUuid: String): Discourse = {
    val query = s"match (start :`${relFactory.startNodeFactory.label}`)-${relationMatcherQuery(relFactory)}->(end :`${relFactory.endNodeFactory.label}` {uuid: {endUuid}}) return start"
    val params = Map("endUuid" -> endUuid)

    Discourse(db.queryGraph(Query(query, params)))
  }

  def connectNodes[START <: UuidNode, RELATION <: SchemaAbstractRelation[START, END] with SchemaItem, END <: UuidNode](startUuid: String, factory: SchemaAbstractRelationFactory[START, RELATION, END], endUuid: String) = {
    val (discourse, (start, end)) = discourseNodes(factory.startNodeFactory, startUuid, factory.endNodeFactory, endUuid)
    discourse.add(factory.local(start, end))
    db.persistChanges(discourse.graph)
    (start, end)
  }

  def disconnectNodes[START <: UuidNode, RELATION <: SchemaAbstractRelation[START,END] with SchemaItem, END <: UuidNode](startUuid: String, factory: SchemaAbstractRelationFactory[START,RELATION,END], endUuid: String) {
    val discourse = relationDiscourseGraph(startUuid, factory, endUuid)

    discourse.graph.nodes --= discourse.contentNodeHyperRelations.map(_.node) //TODO: wrap boilerplate
    discourse.graph.relations.clear()
    db.persistChanges(discourse.graph)
  }
}
