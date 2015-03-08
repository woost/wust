package controllers

import modules.requests.{GoalAddRequest, IdeaAddRequest, NodeAddRequest, ProblemAddRequest}
import play.api.libs.json._
import play.api.mvc.Action
import play.api.mvc.Controller

import renesca._
import renesca.parameter.implicits._
import modules.json.GraphFormat._
import model._

object Ideas extends Controller with ContentNodesController[Idea] {
  override def factory = model.Idea
  override def decodeRequest(jsValue: JsValue) = jsValue.as[IdeaAddRequest]

  def index() = Action {
    Ok(Json.toJson(wholeDiscourseGraph.ideas))
  }

  def showGoals(uuid: String) = Action {
    val query = Query(s"match (:${ Idea.label } {uuid: {uuid}})-[:${ IdeaToReaches.relationType }]->(:${ Reaches.label })-[:${ ReachesToGoal.relationType }]->(goal :${ Goal.label }) return goal", Map("uuid" -> uuid))
    Ok(Json.toJson(db.queryGraph(query).nodes.map(Goal.create)))
  }

  def showProblems(uuid: String) = Action {
    val query = Query(s"match (:${ Idea.label } {uuid: {uuid}})-[:${ IdeaToSolves.relationType }]->(:${ Solves.label })-[:${ SolvesToProblem.relationType }]->(problem :${ Problem.label }) return problem", Map("uuid" -> uuid))
    Ok(Json.toJson(db.queryGraph(query).nodes.map(Problem.create)))
  }
}
