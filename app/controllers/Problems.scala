package controllers

import modules.requests.{IdeaAddRequest, NodeAddRequest, ProblemAddRequest}
import play.api.libs.json._
import play.api.mvc.Action
import play.api.mvc.Controller

import renesca._
import renesca.graph._
import renesca.parameter.implicits._
import modules.json.GraphFormat._
import model._

trait ContentNodesController[NodeType <: ContentNode] extends Controller {
  def factory: ContentNodeFactory[NodeType]
  def decodeRequest(jsValue: JsValue): NodeAddRequest

  val db = new DbService
  db.restService = new RestService("http://localhost:7474")

  def wholeDiscourseGraph: Discourse = {
    Discourse(db.queryGraph("match (n) optional match (n)-[r]-() return n,r"))
  }

  def create = Action { request =>
    val json = request.body.asJson.get
    val nodeAdd = decodeRequest(json)
    val discourse = Discourse.empty
    val contentNode = factory.local
    contentNode.title = nodeAdd.title
    discourse.add(contentNode)
    db.persistChanges(discourse.graph)
    Ok(Json.toJson(contentNode))
  }
}

object Problems extends Controller with ContentNodesController[Problem] {
  override def factory = model.Problem
  override def decodeRequest(jsValue: JsValue) = jsValue.as[ProblemAddRequest]

  def index() = Action {
    Ok(Json.toJson(wholeDiscourseGraph.problems))
  }
}

object Ideas extends Controller with ContentNodesController[Idea] {
  override def factory = model.Idea
  override def decodeRequest(jsValue: JsValue) = jsValue.as[IdeaAddRequest]

  def index() = Action {
    Ok(Json.toJson(wholeDiscourseGraph.ideas))
  }
}
