package controllers

import modules.json.GraphFormat._
import modules.requests._
import play.api.libs.json._
import play.api.mvc.Action

import live.Broadcaster
import renesca._
import renesca.parameter.implicits._
import model.WustSchema._

object Problems extends ContentNodesController[Problem] {
  override def factory = Problem
  override def decodeRequest(jsValue: JsValue) = jsValue.as[ProblemAddRequest]
  val broadcaster = new Broadcaster("problems")

  def index() = Action {
    Ok(Json.toJson(wholeDiscourseGraph.problems))
  }

  def showGoals(uuid: String) = Action {
    val query = Query(s"match (:${Problem.label} {uuid: {uuid}})-[:${Prevents.relationType}]->(goal :${Goal.label}) return goal", Map("uuid" -> uuid))
    Ok(Json.toJson(db.queryGraph(query).nodes.map(Goal.create)))
  }

  def showIdeas(uuid: String) = Action {
    val query = Query(s"match (:${Problem.label} {uuid: {uuid}})<-[:${Solves.endRelationType}]-(:${Solves.label})<-[:${Solves.startRelationType}]-(idea :${Idea.label}) return idea", Map("uuid" -> uuid))
    Ok(Json.toJson(db.queryGraph(query).nodes.map(Idea.create)))
  }

  def showProblems(uuid: String) = Action {
    val query = Query(s"match (:${Problem.label} {uuid: {uuid}})<-[:${Causes.relationType}]-(problem :${Problem.label}) return problem", Map("uuid" -> uuid))
    Ok(Json.toJson(db.queryGraph(query).nodes.map(Problem.create)))
  }

  def connectGoal(uuid: String) = Action(parse.json) { request =>
    val connect = request.body.as[ConnectRequest]

    val (_, goal) = connectNodes(uuid, Prevents, connect.uuid)
    broadcaster.broadcastConnect(uuid, goal)
    Ok(Json.toJson(goal))
  }

  def connectProblem(uuid: String) = Action(parse.json) { request =>
    val connect = request.body.as[ConnectRequest]

    val (cause, _) = connectNodes(connect.uuid, Causes, uuid)
    broadcaster.broadcastConnect(uuid, cause)
    Ok(Json.toJson(cause))
  }

  def connectIdea(uuid: String) = Action(parse.json) { request =>
    val connect = request.body.as[ConnectRequest]

    val (idea, _) = connectNodes(connect.uuid, Solves, uuid)
    broadcaster.broadcastConnect(uuid, idea)
    Ok(Json.toJson(idea))
  }

  def disconnectGoal(uuid: String, uuidGoal: String) = Action {
    disconnectNodes(uuid, Prevents, uuidGoal)
    broadcaster.broadcastDisconnect(uuid, uuidGoal, "GOAL")
    Ok(JsObject(Seq()))
  }

  def disconnectProblem(uuid: String, uuidProblem: String) = Action {
    disconnectNodes(uuidProblem, Causes, uuid)
    broadcaster.broadcastDisconnect(uuid, uuidProblem, "PROBLEM")
    Ok(JsObject(Seq()))
  }

  def disconnectIdea(uuid: String, uuidIdea: String) = Action {
    disconnectNodes(uuidIdea, Solves, uuid)
    broadcaster.broadcastDisconnect(uuid, uuidIdea, "IDEA")
    Ok(JsObject(Seq()))
  }
}

