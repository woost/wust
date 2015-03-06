package controllers

import play.api.libs.json._
import play.api.mvc.Action
import play.api.mvc.Controller

import renesca._
import renesca.graph._
import renesca.parameter.implicits._
import modules.json.GraphFormat._
import model._

object Graphs extends Controller {
  val db = new DbService
  db.restService = new RestService("http://localhost:7474")

  def index() = Action {
    val graph = db.queryGraph("match (n) optional match (n)-[r]-() return n,r")
    val discourse = Discourse(graph)
    Ok(Json.toJson(discourse))
  }

  def remove(id: String) = Action{
    val graph = db.queryGraph(Query("match (n) where n.uuid = {uuid} return n", Map("uuid" -> id)))
    graph.nodes.clear
    db.persistChanges(graph)
    Ok(JsObject(Seq()))
  }

  def show(id: String) = Action {
    val graph = db.queryGraph(Query("match (n) where n.uuid = {uuid} return n", Map("uuid" -> id)))
    val nodes = Discourse(graph).discourseNodes
    if (nodes.isEmpty) {
      BadRequest
    } else {
      Ok(Json.toJson(nodes.head))
    }
  }
}
