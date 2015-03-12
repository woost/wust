package controllers

import model._
import modules.json.GraphFormat._
import modules.requests.{GoalAddRequest, ProblemAddRequest, IdeaAddRequest}
import play.api.libs.json._
import play.api.mvc.{Action, Controller}
import renesca._
import renesca.parameter.implicits._

object Goals extends Controller with ContentNodesController[Goal] {
  override def factory = model.Goal
  override def decodeRequest(jsValue: JsValue) = jsValue.as[GoalAddRequest]

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

  def connectProblem(uuid: String, uuidProblem: String) = Action {
    val discourse = nodeDiscourseGraph(List(uuid, uuidProblem))
    val goal = discourse.goals.find(p => p.uuid == uuid).get
    val problem = discourse.problems.find(p => p.uuid == uuidProblem).get

    discourse.add(Prevents.local(problem, goal))

    db.persistChanges(discourse.graph)
    Ok(Json.toJson(problem))
  }

  def connectIdea(uuid: String, uuidIdea: String) = Action {
    val discourse = nodeDiscourseGraph(List(uuid, uuidIdea))
    val goal = discourse.goals.find(p => p.uuid == uuid).get
    val idea = discourse.ideas.find(p => p.uuid == uuidIdea).get
    val reaches = Reaches.local

    discourse.add(reaches)
    discourse.add(ReachesToGoal.local(reaches, goal))
    discourse.add(IdeaToReaches.local(idea, reaches))

    db.persistChanges(discourse.graph)
    Ok(Json.toJson(idea))
  }

  def disconnectProblem(uuid: String, uuidProblem: String) = Action {
    val discourse = relationDiscourseGraph(uuidProblem, Prevents.relationType, uuid)
    discourse.graph.relations.clear()
    db.persistChanges(discourse.graph)
    Ok(JsObject(Seq()))
  }
}
