package controllers

import model.WustSchema._
import model._
import renesca._
import renesca.schema.SchemaNode
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

  private def nodeWithUuid[NODE <: SchemaNode](discourse: Discourse, uuid: String) = discourse.uuidNodes.find(_.uuid == uuid).get.asInstanceOf[NODE]

  def wholeDiscourseGraph: Discourse = {
    Discourse(db.queryGraph("match (n) optional match (n)-[r]-() return n,r"))
  }

  def nodeDiscourseGraph(uuids: String*): Discourse = {
    if(uuids.isEmpty)
      return Discourse.empty

    Discourse(db.queryGraph(Query("match (n) where n.uuid in {uuids} return *", Map("uuids" -> uuids))))
  }

  def discourseNodes[NODE <: SchemaNode](uuids: String*) = {
    val discourse = nodeDiscourseGraph(uuids:_*)
    (discourse, uuids.map {uuid => nodeWithUuid[NODE](discourse, uuid)})
  }

  def discourseNodes[START <: SchemaNode, END <: SchemaNode](startUuid: String, endUuid: String) = {
    val discourse = nodeDiscourseGraph(startUuid, endUuid)
    (discourse, (nodeWithUuid[START](discourse, startUuid), nodeWithUuid[END](discourse, endUuid)))
  }

  def relationDiscourseGraph(startUuid: String, relationType: RelationType, endUuid: String): Discourse = {
    relationDiscourseGraph(startUuid, List(relationType), endUuid)
  }

  def relationDiscourseGraph(startUuid: String, relationTypes: Seq[RelationType], endUuid: String): Discourse = {
    if(relationTypes.isEmpty)
      return Discourse.empty

    val relationMatcher = relationTypes.map(rel => s"-[`$rel`]->").mkString("()")
    Discourse(db.queryGraph(Query(s"match (from {uuid: {startUuid}})${ relationMatcher }(to {uuid: {endUuid}}) return *",
      Map("startUuid" -> startUuid, "endUuid" -> endUuid))))
  }
}
