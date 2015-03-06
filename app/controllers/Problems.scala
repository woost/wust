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
  def label = factory.label
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

  def show(uuid: String) = Action {
    val query = Query(s"match (n :$label {uuid: {uuid}}) return n limit 1", Map("uuid" -> uuid))
    db.queryGraph(query).nodes.headOption match {
      case Some(node) => Ok(Json.toJson(factory.create(node)))
      case None       => BadRequest(s"Node with label $label and uuid $uuid not found.")
    }
  }

}

object Problems extends Controller with ContentNodesController[Problem] {
  override def factory = model.Problem
  override def decodeRequest(jsValue: JsValue) = jsValue.as[ProblemAddRequest]

  def index() = Action {
    Ok(Json.toJson(wholeDiscourseGraph.problems))
  }
  def showGoals(uuid: String) = Action {
    //TODO: relationtypes in query
    val query = Query(s"match (:${ Problem.label } {uuid: {uuid}})-->(:${ ProblemGoal.label })-->(goal :${ Goal.label }) return goal", Map("uuid" -> uuid))
    Ok(Json.toJson(db.queryGraph(query).nodes.map(Goal.create)))
  }

  def showIdeas(uuid: String) = Action {
    //TODO: relationtypes in query
    val query = Query(s"match (:${ Problem.label } {uuid: {uuid}})-->(:${ ProblemGoal.label })-->(:${ IdeaProblemGoal.label })-->(idea :${ Idea.label }) return idea", Map("uuid" -> uuid))
    Ok(Json.toJson(db.queryGraph(query).nodes.map(Idea.create)))
  }
}

object Ideas extends Controller with ContentNodesController[Idea] {
  override def factory = model.Idea
  override def decodeRequest(jsValue: JsValue) = jsValue.as[IdeaAddRequest]

  def index() = Action {
    Ok(Json.toJson(wholeDiscourseGraph.ideas))
  }
}
