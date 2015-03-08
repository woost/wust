package controllers

import model._
import modules.json.GraphFormat._
import modules.requests.IdeaAddRequest
import play.api.libs.json._
import play.api.mvc.{Action, Controller}
import renesca._
import renesca.parameter.implicits._

object Goals extends Controller with ContentNodesController[Idea] {
  override def factory = model.Idea
  override def decodeRequest(jsValue: JsValue) = jsValue.as[IdeaAddRequest]

  def index() = Action {
    Ok(Json.toJson(wholeDiscourseGraph.goals))
  }

  def showIdeas(uuid: String) = Action {
    val query = Query(s"match (:${ Goal.label } {uuid: {uuid}})<-[:${ ReachesToGoal.relationType }]-(:${ Reaches.label })<-[:${ IdeaToReaches.relationType }]-(idea :${ Idea.label }) return idea", Map("uuid" -> uuid))
    Ok(Json.toJson(db.queryGraph(query).nodes.map(Goal.create)))
  }

  def showProblems(uuid: String) = Action {
    val query = Query(s"match (:${ Goal.label } {uuid: {uuid}})<-[:${ Prevents.relationType }]-(problem :${ Problem.label }) return problem", Map("uuid" -> uuid))
    Ok(Json.toJson(db.queryGraph(query).nodes.map(Problem.create)))
  }
}
