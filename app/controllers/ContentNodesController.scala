package controllers

import modules.requests.{GoalAddRequest, IdeaAddRequest, NodeAddRequest, ProblemAddRequest}
import play.api.libs.json._
import play.api.mvc.Action
import play.api.mvc.Controller

import renesca._
import renesca.parameter.implicits._
import modules.json.GraphFormat._
import model._

trait ContentNodesController[NodeType <: ContentNode] extends Controller {
  def factory: ContentNodeFactory[NodeType]
  def label = factory.label
  def decodeRequest(jsValue: JsValue): NodeAddRequest

  val db = new DbService
  db.restService = new RestService("http://localhost:7474")

  def wholeDiscourseGraph: Discourse = {
    Discourse(db.queryGraph("match (n) optional match (n)-[r]-() return n,r"))
  }

  def nodeDiscourseGraph(uuid: String): Discourse = {
    Discourse(db.queryGraph(Query(s"match (node {uuid: {uuid}}) return node limit 1", Map("uuid" -> uuid))))
  }

  def create = Action { request =>
    val json = request.body.asJson.get
    val nodeAdd = decodeRequest(json)
    val discourse = Discourse.empty
    val contentNode = factory.local(nodeAdd.title)
    discourse.add(contentNode)
    db.persistChanges(discourse.graph)
    Ok(Json.toJson(contentNode))
  }

  def show(uuid: String) = Action {
    val query = Query(s"match (n :$label {uuid: {uuid}}) return n limit 1", Map("uuid" -> uuid))
    db.queryGraph(query).nodes.headOption match {
      case Some(node) => Ok(Json.toJson(factory.create(node)))
      case None       => BadRequest(s"Node with label $label and uuid $uuid not found.")
    }
  }

}
