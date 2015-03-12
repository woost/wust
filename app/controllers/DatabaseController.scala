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
  db.restService = new RestService("db.neo4j.url".configOrElse("http://localhost:7474"))

  def wholeDiscourseGraph: Discourse = {
    Discourse(db.queryGraph("match (n) optional match (n)-[r]-() return n,r"))
  }

  def nodeDiscourseGraph(uuid: String): Discourse = {
    nodeDiscourseGraph(List(uuid))
  }

  def nodeDiscourseGraph(uuids: Seq[String]): Discourse = {
    if(uuids.isEmpty)
      return Discourse.empty

    val uuidMap: ParameterMap = uuids.zipWithIndex.map { case (uuid, i) => s"node_$i" -> uuid }.toMap
    val nodeMatcher = uuidMap.keys.map(key => s"($key {uuid: {$key}})").mkString(",")

    Discourse(db.queryGraph(Query(s"match $nodeMatcher return *", uuidMap)))
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
