package controllers

import play.api.libs.json._
import play.api.mvc.Action
import play.api.mvc.Controller

import renesca._
import renesca.graph._
import renesca.parameter.implicits._
import modules.json.GraphFormat._

object Graphs extends Controller {
  val db = new DbService
  db.restService = new RestService("http://localhost:7474")

  def index() = Action {
    val graph = db.queryGraph("match (n) optional match (n)-[r]-() return n,r")
    Ok(Json.toJson(graph))
  }

  def remove(id: String) = Action{
    val graph = db.queryGraph(Query("match (n) where id(n) = {id} return n", Map("id" -> id.toInt)))
    graph.nodes.clear
    db.persistChanges(graph)
    Ok(JsObject(Seq()))
  }

  def create() = Action { request =>
    val json = request.body.asJson.get

    val graph = Graph.empty
    val node = (json \ "node").as[Node]
    graph.nodes += node

    val referenceOpt = (json \ "reference").asOpt[String]
    var relationOpt:Option[Relation] = None
    for (reference <- referenceOpt) {
      val nodes = db.queryGraph(Query("match (n) where id(n) = {id} return n", Map("id" -> reference.toInt))).nodes
      if (!nodes.isEmpty) {
        val refNode = nodes.head
        val label = (json \ "label").as[String]
        val relation = Relation.local(node, refNode, label)
        graph.relations += relation
        relationOpt = Some(relation)
      }
    }

    db.persistChanges(graph)
    Ok(JsObject(
      Seq(
        ("node", Json.toJson(node)),
        ("relation", (relationOpt match {
          case Some(relation) => Json.toJson(relation)
          case None => JsNull
        }))
      )
    ))
  }

  def show(id: String) = Action {
    val nodes = db.queryGraph(Query("match (n) where id(n) = {id} return n", Map("id" -> id.toInt))).nodes
    if (nodes.isEmpty) {
      BadRequest
    } else {
      val node = nodes.head
      val ideas = db.queryGraph(Query("match (n:IDEA)-[r:SOLVES]->(m) where id(m) = {id} return n", Map("id" -> id.toInt))).nodes
      val questions = db.queryGraph(Query("match (n:QUESTION)-[r:ASKS]->(m) where id(m) = {id} return n", Map("id" -> id.toInt))).nodes
      Ok(Json.toJson(JsObject(
        Seq(
          ("node", Json.toJson(node)),
          ("ideas", Json.toJson(ideas)),
          ("questions", Json.toJson(questions))
        )
      )))
    }
  }
}
