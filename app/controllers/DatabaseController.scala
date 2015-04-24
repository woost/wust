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

trait DatabaseController {
  val db = new DbService
  db.restService = new RestService(
    server = "db.neo4j.url".configOrElse("http://localhost:7474"),
    credentials = Some(spray.http.BasicHttpCredentials("db.neo4j.user".configOrElse("neo4j"),"db.neo4j.pass".configOrElse("neo4j")))
  )

  private def nodeWithUuid[NODE <: UuidNode](discourse: Discourse, uuid: String) = discourse.uuidNodes.find(_.uuid == uuid).get.asInstanceOf[NODE]

  def wholeDiscourseGraph: Discourse = {
    Discourse(db.queryGraph("match (n) optional match (n)-[r]-() return n,r"))
  }

  def nodeDiscourseGraph[START <: UuidNode, END <: UuidNode](startFactory: SchemaNodeFactory[START], startUuid: String, endFactory: SchemaNodeFactory[END], endUuid: String): Discourse = {
    val query = s"match (start :`${startFactory.label}` {uuid: {startUuid}}), (end: `${endFactory.label}` {uuid: {endUuid}}) return start,end limit 2"
    val params = Map("startUuid" -> startUuid, "endUuid" -> endUuid)

    Discourse(db.queryGraph(Query(query, params)))
  }

  def nodeDiscourseGraph[NODE <: UuidNode](factory: SchemaNodeFactory[NODE], uuids: String*): Discourse = {
    if(uuids.isEmpty)
      return Discourse.empty

    val query = s"match (n :`${factory.label}`) where n.uuid in {uuids} return n limit ${uuids.size}"
    val params = Map("uuids" -> uuids)

    Discourse(db.queryGraph(Query(query, params)))
  }

  def discourseNodes[NODE <: UuidNode](factory: SchemaNodeFactory[NODE], uuids: String*) = {
    val discourse = nodeDiscourseGraph(factory, uuids:_*)
    (discourse, uuids.map {uuid => nodeWithUuid[NODE](discourse, uuid)})
  }

  def discourseNodes[START <: UuidNode, END <: UuidNode](startFactory: SchemaNodeFactory[START], startUuid: String, endFactory: SchemaNodeFactory[END], endUuid: String) = {
    val discourse = nodeDiscourseGraph(startFactory, startUuid, endFactory, endUuid)
    (discourse, (nodeWithUuid[START](discourse, startUuid), nodeWithUuid[END](discourse, endUuid)))
  }

  def relationDiscourseGraph[START <: UuidNode, RELATION <: SchemaRelation[START,END], END <: UuidNode](startUuid: String, relFactory: SchemaRelationFactory[START,RELATION,END], endUuid: String): Discourse = {
    val query = s"match (start :`${relFactory.startNodeFactory.label}` {uuid: {startUuid}})-[relation :`${relFactory.relationType}`]->(end :`${relFactory.endNodeFactory.label}` {uuid: {endUuid}}) return *"
    val params = Map("startUuid" -> startUuid, "endUuid" -> endUuid)

    Discourse(db.queryGraph(Query(query, params)))
  }

  def relationDiscourseGraph[START <: UuidNode, RELATION <: SchemaHyperRelation[START,_,RELATION,_,END], END <: UuidNode](startUuid: String, relFactory: SchemaHyperRelationFactory[START,_,RELATION,_,END], endUuid: String): Discourse = {
    val query = s"match (start :`${relFactory.startNodeFactory.label}` {uuid: {startUuid}})-[startRelation :`${relFactory.startRelationType}`]->(middle :`${relFactory.label}`)-[endRelation :`${relFactory.endRelationType}`]->(end :`${relFactory.endNodeFactory.label}` {uuid: {endUuid}}) return *"
    val params = Map("startUuid" -> startUuid, "endUuid" -> endUuid)

    Discourse(db.queryGraph(Query(query, params)))
  }

  def connectedNodesDiscourseGraph[START <: UuidNode, RELATION <: SchemaRelation[START,_]](startUuid: String, relFactory: SchemaRelationFactory[START,RELATION,_]): Discourse = {
    val query = s"match (start :`${relFactory.startNodeFactory.label}` {uuid: {startUuid}})-[:`${relFactory.relationType}`]->(end :`${relFactory.endNodeFactory.label}`) return end"
    val params = Map("startUuid" -> startUuid)

    Discourse(db.queryGraph(Query(query, params)))
  }

  def connectedNodesDiscourseGraph[RELATION <: SchemaRelation[_,END], END <: UuidNode](relFactory: SchemaRelationFactory[_,RELATION,END], endUuid: String): Discourse = {
    val query = s"match (start :`${relFactory.startNodeFactory.label}`)-[:`${relFactory.relationType}`]->(end :`${relFactory.endNodeFactory.label}` {uuid: {endUuid}}) return start"
    val params = Map("endUuid" -> endUuid)

    Discourse(db.queryGraph(Query(query, params)))
  }

  def connectedNodesDiscourseGraph[START <: UuidNode, RELATION <: SchemaHyperRelation[START,_,RELATION,_,_]](startUuid: String, relFactory: SchemaHyperRelationFactory[START,_,RELATION,_,_]): Discourse = {
    val query = s"match (start :`${relFactory.startNodeFactory.label}` {uuid: {startUuid}})-[:`${relFactory.startRelationType}`]->(middle :`${relFactory.factory.label}`)-[:`${relFactory.endRelationType}`]->(end :`${relFactory.endNodeFactory.label}`) return end"
    val params = Map("startUuid" -> startUuid)

    Discourse(db.queryGraph(Query(query, params)))
  }

  def connectedNodesDiscourseGraph[RELATION <: SchemaHyperRelation[_,_,RELATION,_,END], END <: UuidNode](relFactory: SchemaHyperRelationFactory[_,_,RELATION,_,END], endUuid: String): Discourse = {
    val query = s"match (start :`${relFactory.startNodeFactory.label}`)-[:`${relFactory.startRelationType}`]->(middle :`${relFactory.factory.label}`)-[:`${relFactory.endRelationType}`]->(end :`${relFactory.endNodeFactory.label}` {uuid: {endUuid}}) return start"
    val params = Map("endUuid" -> endUuid)

    Discourse(db.queryGraph(Query(query, params)))
  }
}
