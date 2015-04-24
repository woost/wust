package controllers

import modules.json.GraphFormat._
import modules.requests._
import play.api.libs.json._
import play.api.mvc.Action

import live.Broadcaster
import renesca._
import renesca.parameter.implicits._
import model.WustSchema._

object Ideas extends ContentNodesController[Idea] {
  override def factory = Idea
  override def decodeRequest(jsValue: JsValue) = jsValue.as[IdeaAddRequest]
  val broadcaster = new Broadcaster("ideas")

  def index() = Action {
    Ok(Json.toJson(wholeDiscourseGraph.ideas))
  }

  def showGoals(uuid: String) = Action {
    val discourse = connectedNodesDiscourseGraph(uuid, Achieves)
    Ok(Json.toJson(discourse.goals))
  }

  def showProblems(uuid: String) = Action {
    val discourse = connectedNodesDiscourseGraph(uuid, Solves)
    Ok(Json.toJson(discourse.problems))
  }

  def showIdeas(uuid: String) = Action {
    val discourse = connectedNodesDiscourseGraph(SubIdea, uuid)
    Ok(Json.toJson(discourse.ideas))
  }

  def connectGoal(uuid: String) = Action(parse.json) { request =>
    val connect = request.body.as[ConnectRequest]

    val (_, goal) = connectNodes(uuid, Achieves, connect.uuid)
    broadcaster.broadcastConnect(uuid, goal)
    Ok(Json.toJson(goal))
  }

  def connectProblem(uuid: String) = Action(parse.json) { request =>
    val connect = request.body.as[ConnectRequest]

    val (_, problem) = connectNodes(uuid, Solves, connect.uuid)
    broadcaster.broadcastConnect(uuid, problem)
    Ok(Json.toJson(problem))
  }

  def connectIdea(uuid: String) = Action(parse.json) { request =>
    val connect = request.body.as[ConnectRequest]

    val (subIdea, _) = connectNodes(connect.uuid, SubIdea, uuid)
    broadcaster.broadcastConnect(uuid, subIdea)
    Ok(Json.toJson(subIdea))
  }

  def disconnectGoal(uuid: String, uuidGoal: String) = Action {
    disconnectNodes(uuid, Achieves, uuidGoal)
    broadcaster.broadcastDisconnect(uuid, uuidGoal, "GOAL")
    Ok(JsObject(Seq()))
  }

  def disconnectProblem(uuid: String, uuidProblem: String) = Action {
    disconnectNodes(uuid, Solves, uuidProblem)
    broadcaster.broadcastDisconnect(uuid, uuidProblem, "PROBLEM")
    Ok(JsObject(Seq()))
  }

  def disconnectIdea(uuid: String, uuidIdea: String) = Action {
    disconnectNodes(uuid, SubIdea, uuidIdea)
    broadcaster.broadcastDisconnect(uuid, uuidIdea, "IDEA")
    Ok(JsObject(Seq()))
  }
}
