package controllers

import model._
import renesca._
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

  def wholeDiscourseGraph: Discourse = {
    Discourse(db.queryGraph("match (n) optional match (n)-[r]-() return n,r"))
  }

  def nodeDiscourseGraph(uuid: String): Discourse = {
    nodeDiscourseGraph(List(uuid))
  }

  def nodeDiscourseGraph(uuids: Seq[String]): Discourse = {
    if(uuids.isEmpty)
      return Discourse.empty

    Discourse(db.queryGraph(Query("match (n) where n.uuid in {uuids} return *", Map("uuids" -> uuids))))
  }

  def relationDiscourseGraph(uuidFrom: String, relationType: RelationType, uuidTo: String): Discourse = {
    relationDiscourseGraph(uuidFrom, List(relationType), uuidTo)
  }

  def relationDiscourseGraph(uuidFrom: String, relationTypes: Seq[RelationType], uuidTo: String): Discourse = {
    if(relationTypes.isEmpty)
      return Discourse.empty

    val relationMatcher = relationTypes.map(rel => s"-[`$rel`]->").mkString("()")
    Discourse(db.queryGraph(Query(s"match (from {uuid: {uuidFrom}})${ relationMatcher }(to {uuid: {uuidTo}}) return *",
      Map("uuidFrom" -> uuidFrom, "uuidTo" -> uuidTo))))
  }
}
