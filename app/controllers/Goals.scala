package controllers

import modules.json.GraphFormat._
import modules.requests._
import play.api.libs.json._
import play.api.mvc.Action

import live.Broadcaster
import renesca._
import renesca.parameter.implicits._
import model.WustSchema._

object Goals extends ContentNodesController[Goal] {
  override def factory = Goal
  override def decodeRequest(jsValue: JsValue) = jsValue.as[GoalAddRequest]
  val broadcaster = new Broadcaster("goals")

  def index() = Action {
    Ok(Json.toJson(wholeDiscourseGraph.goals))
  }

  def showGoals(uuid: String) = Action {
    val query = Query(s"match (:${ Goal.label } {uuid: {uuid}})<-[:${ SubGoal.relationType }]-(goal :${ Goal.label }) return goal", Map("uuid" -> uuid))
    Ok(Json.toJson(db.queryGraph(query).nodes.map(Goal.create)))
  }

  def showProblems(uuid: String) = Action {
    val query = Query(s"match (:${ Goal.label } {uuid: {uuid}})<-[:${ Prevents.relationType }]-(problem :${ Problem.label }) return problem", Map("uuid" -> uuid))
    Ok(Json.toJson(db.queryGraph(query).nodes.map(Problem.create)))
  }

  def showIdeas(uuid: String) = Action {
    val query = Query(s"match (:${ Goal.label } {uuid: {uuid}})<-[:${ Achieves.endRelationType }]-(:${ Achieves.label })<-[:${ Achieves.startRelationType }]-(idea :${ Idea.label }) return idea", Map("uuid" -> uuid))
    Ok(Json.toJson(db.queryGraph(query).nodes.map(Idea.create)))
  }

  def connectGoal(uuid: String) = Action(parse.json) { request =>
    val connect = request.body.as[ConnectRequest]

    val (subGoal, _) = connectNodes(connect.uuid, SubGoal, uuid)
    broadcaster.broadcastConnect(uuid, subGoal)
    Ok(Json.toJson(subGoal))
  }

  def connectProblem(uuid: String) = Action(parse.json) { request =>
    val connect = request.body.as[ConnectRequest]

    val (problem, _) = connectNodes(connect.uuid, Prevents, uuid)
    broadcaster.broadcastConnect(uuid, problem)
    Ok(Json.toJson(problem))
  }

  def connectIdea(uuid: String) = Action(parse.json) { request =>
    val connect = request.body.as[ConnectRequest]

    val (idea, _) = connectNodes(connect.uuid, Achieves, uuid)
    broadcaster.broadcastConnect(uuid, idea)
    Ok(Json.toJson(idea))
  }

  def disconnectGoal(uuid: String, uuidGoal: String) = Action {
    disconnectNodes(uuidGoal, SubGoal, uuid)
    broadcaster.broadcastDisconnect(uuid, uuidGoal, "GOAL")
    Ok(JsObject(Seq()))
  }

  def disconnectProblem(uuid: String, uuidProblem: String) = Action {
    disconnectNodes(uuidProblem, Prevents, uuid)
    broadcaster.broadcastDisconnect(uuid, uuidProblem, "PROBLEM")
    Ok(JsObject(Seq()))
  }

  def disconnectIdea(uuid: String, uuidIdea: String) = Action {
    disconnectNodes(uuidIdea, Achieves, uuid)
    broadcaster.broadcastDisconnect(uuid, uuidIdea, "IDEA")
    Ok(JsObject(Seq()))
  }
}
