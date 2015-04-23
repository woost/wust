package controllers

import org.atmosphere.play.AtmosphereCoordinator.{instance => atmosphere}
import modules.requests._
import play.api.libs.json._
import play.api.mvc.Action

import renesca._
import renesca.parameter.implicits._
import modules.json.GraphFormat._
import model.WustSchema._
import spray.can.Http.ConnectionAttemptFailedException

object Problems extends ContentNodesController[Problem] {
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

    val (_, goal) = connectNodes(uuid, connect.uuid, Prevents)
    broadcastConnect(uuid, goal)
    Ok(Json.toJson(goal))
  }

  def connectProblem(uuid: String) = Action(parse.json) { request =>
    val connect = request.body.as[ConnectRequest]

    val (cause, _) = connectNodes(connect.uuid, uuid, Causes)
    broadcastConnect(uuid, cause)
    Ok(Json.toJson(cause))
  }

  def connectIdea(uuid: String) = Action(parse.json) { request =>
    val connect = request.body.as[ConnectRequest]

    val (idea, _) = connectNodes(connect.uuid, uuid, Solves)
    broadcastConnect(uuid, idea)
    Ok(Json.toJson(idea))
  }

  def disconnectGoal(uuid: String, uuidGoal: String) = Action {
    disconnectNodes(uuid, Prevents.relationType, uuidGoal)
    broadcastDisconnect(uuid, uuidGoal, "GOAL")
    Ok(JsObject(Seq()))
  }

  def disconnectProblem(uuid: String, uuidProblem: String) = Action {
    disconnectNodes(uuidProblem, Causes.relationType, uuid)
    broadcastDisconnect(uuid, uuidProblem, "PROBLEM")
    Ok(JsObject(Seq()))
  }

  def disconnectIdea(uuid: String, uuidIdea: String) = Action {
    disconnectNodes(uuidIdea, List(Solves.startRelationType, Solves.endRelationType), uuid)
    broadcastDisconnect(uuid, uuidIdea, "IDEA")
    Ok(JsObject(Seq()))
  }
}

