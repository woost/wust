package controllers

import org.atmosphere.play.AtmosphereCoordinator.{instance => atmosphere}
import modules.requests.{GoalAddRequest, IdeaAddRequest, NodeAddRequest, ProblemAddRequest}
import play.api.libs.json._
import play.api.mvc.Action
import play.api.mvc.Controller

import renesca._
import renesca.parameter.implicits._
import modules.json.GraphFormat._
import model._

object Problems extends Controller with ContentNodesController[Problem] {
  override def factory = model.Problem
  override def decodeRequest(jsValue: JsValue) = jsValue.as[ProblemAddRequest]

  def index() = Action {
    Ok(Json.toJson(wholeDiscourseGraph.problems))
  }

  def showGoals(uuid: String) = Action {
    val query = Query(s"match (:${ Problem.label } {uuid: {uuid}})-[:${ Prevents.relationType }]->(goal :${ Goal.label }) return goal", Map("uuid" -> uuid))
    Ok(Json.toJson(db.queryGraph(query).nodes.map(Goal.create)))
  }

  def showIdeas(uuid: String) = Action {
    val query = Query(s"match (:${ Problem.label } {uuid: {uuid}})<-[:${ SolvesToProblem.relationType }]-(:${ Solves.label })<-[:${ IdeaToSolves.relationType }]-(idea :${ Idea.label }) return idea", Map("uuid" -> uuid))
    Ok(Json.toJson(db.queryGraph(query).nodes.map(Idea.create)))
  }

  def showProblems(uuid: String) = Action {
    val query = Query(s"match (:${ Problem.label } {uuid: {uuid}})<-[:${ Causes.relationType }]-(problem :${ Problem.label }) return problem", Map("uuid" -> uuid))
    Ok(Json.toJson(db.queryGraph(query).nodes.map(Problem.create)))
  }

  def connectGoal(uuid: String, uuidGoal: String) = Action {
    val discourse = nodeDiscourseGraph(List(uuid, uuidGoal))
    val problem = discourse.problems.find(p => p.uuid == uuid).get
    val goal = discourse.goals.find(p => p.uuid == uuidGoal).get

    discourse.add(Prevents.local(problem, goal))

    //TODO: shared code: label <-> api mapping

    // broadcast change to subscribed atmosphere clients
    val broadcaster = atmosphere.framework.metaBroadcaster
    broadcaster.broadcastTo(s"/live/v1/problems/$uuid", JsObject(Seq(("type",JsString("create")),("data",Json.toJson(goal)))))

    db.persistChanges(discourse.graph)
    Ok(Json.toJson(goal))
  }

  def connectProblem(uuid: String, uuidProblem: String) = Action {
    val discourse = nodeDiscourseGraph(List(uuid, uuidProblem))
    val consequence = discourse.problems.find(p => p.uuid == uuid).get
    val cause = discourse.problems.find(p => p.uuid == uuidProblem).get

    discourse.add(Causes.local(cause, consequence))

    db.persistChanges(discourse.graph)
    Ok(Json.toJson(cause))
  }

  def connectIdea(uuid: String, uuidIdea: String) = Action {
    val discourse = nodeDiscourseGraph(List(uuid, uuidIdea))
    val problem = discourse.problems.find(p => p.uuid == uuid).get
    val idea = discourse.ideas.find(p => p.uuid == uuidIdea).get
    val solves = Solves.local

    discourse.add(solves)
    discourse.add(SolvesToProblem.local(solves, problem))
    discourse.add(IdeaToSolves.local(idea, solves))

    db.persistChanges(discourse.graph)
    Ok(Json.toJson(idea))
  }

  def disconnectGoal(uuid: String, uuidGoal: String) = {
    disconnect(uuid, Prevents.relationType, uuidGoal)
  }

  def disconnectProblem(uuid: String, uuidProblem: String) = {
    disconnect(uuidProblem, Causes.relationType, uuid)
  }

  def disconnectIdea(uuid: String, uuidIdea: String) = {
    disconnect(uuidIdea, List(IdeaToSolves.relationType, SolvesToProblem.relationType), uuid)
  }
}

