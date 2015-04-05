package controllers

import org.atmosphere.play.AtmosphereCoordinator.{instance => atmosphere}
import modules.requests.{GoalAddRequest, IdeaAddRequest, NodeAddRequest, ProblemAddRequest}
import play.api.libs.json._
import play.api.mvc.Action
import play.api.mvc.Controller

import renesca._
import renesca.parameter.implicits._
import modules.json.GraphFormat._
import model.WustSchema._
import spray.can.Http.ConnectionAttemptFailedException

object Problems extends Controller with ContentNodesController[Problem] {
  override def factory = Problem
  override def apiname = "problems"
  override def decodeRequest(jsValue: JsValue) = jsValue.as[ProblemAddRequest]

  def index() = Action {
    try {
      Ok(Json.toJson(wholeDiscourseGraph.problems))
    } catch {
      case _: ConnectionAttemptFailedException => InternalServerError("No database connectivity.")
    }
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
    db.persistChanges(discourse.graph)

    broadcast(uuid, jsonChange("connect", Json.toJson(goal)))

    Ok(Json.toJson(goal))
  }

  def connectProblem(uuid: String, uuidProblem: String) = Action {
    val discourse = nodeDiscourseGraph(List(uuid, uuidProblem))
    val consequence = discourse.problems.find(p => p.uuid == uuid).get
    val cause = discourse.problems.find(p => p.uuid == uuidProblem).get

    discourse.add(Causes.local(cause, consequence))
    db.persistChanges(discourse.graph)

    broadcast(uuid, jsonChange("connect", Json.toJson(cause)))

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

    broadcast(uuid, jsonChange("connect", Json.toJson(idea)))

    Ok(Json.toJson(idea))
  }

  def disconnectGoal(uuid: String, uuidGoal: String) = Action {
    disconnect(uuid, Prevents.relationType, uuidGoal)
    broadcastDisconnect(uuid, uuidGoal, "GOAL")
    Ok(JsObject(Seq()))
  }

  def disconnectProblem(uuid: String, uuidProblem: String) = Action {
    disconnect(uuidProblem, Causes.relationType, uuid)
    broadcastDisconnect(uuid, uuidProblem, "PROBLEM")
    Ok(JsObject(Seq()))
  }

  def disconnectIdea(uuid: String, uuidIdea: String) = Action {
    disconnect(uuidIdea, List(IdeaToSolves.relationType, SolvesToProblem.relationType), uuid)
    broadcastDisconnect(uuid, uuidIdea, "IDEA")
    Ok(JsObject(Seq()))
  }
}

