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
    val discourse = connectedNodesDiscourseGraph(SubGoal, uuid)
    Ok(Json.toJson(discourse.goals))
  }

  def showProblems(uuid: String) = Action {
    val discourse = connectedNodesDiscourseGraph(Prevents, uuid)
    Ok(Json.toJson(discourse.problems))
  }

  def showIdeas(uuid: String) = Action {
    val discourse = connectedNodesDiscourseGraph(Achieves, uuid)
    Ok(Json.toJson(discourse.ideas))
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
