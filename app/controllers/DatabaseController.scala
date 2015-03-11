package controllers

import model._
import renesca._
import renesca.graph.RelationType
import renesca.parameter.implicits._

trait DatabaseController {
  val db = new DbService
  db.restService = new RestService("http://localhost:7474")

  def wholeDiscourseGraph: Discourse = {
    Discourse(db.queryGraph("match (n) optional match (n)-[r]-() return n,r"))
  }

  def nodeDiscourseGraph(uuid: String): Discourse = {
    Discourse(db.queryGraph(Query(s"match (node {uuid: {uuid}}) return node limit 1", Map("uuid" -> uuid))))
  }

  def relationDiscourseGraph(uuidFrom: String, relationType: RelationType, uuidTo: String): Discourse = {
    Discourse(db.queryGraph(Query(s"match (from {uuid: {uuidFrom}})-[r:${relationType}]-(to {uuid: {uuidTo}}) return from, r, to", Map("uuidFrom" -> uuidFrom, "uuidTo" -> uuidTo))))
  }
}
