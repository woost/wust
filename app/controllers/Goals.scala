package controllers

import model._
import modules.json.GraphFormat._
import modules.requests._
import play.api.libs.json._
import play.api.mvc.{Action, Controller}
import renesca._
import renesca.parameter.implicits._
import model.WustSchema._

object Goals extends Controller with ContentNodesController[Goal] {
  override def factory = Goal
  override def apiname = "goals"
  override def decodeRequest(jsValue: JsValue) = jsValue.as[GoalAddRequest]

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

  def connectGoal(uuid: String) = Action { request =>
    val json = request.body.asJson.get
    val connect = json.as[ConnectRequest]

    val (discourse, Seq(goal: Goal, subGoal: Goal)) = discourseNodes(uuid, connect.uuid)
    discourse.add(SubGoal.local(subGoal, goal))
    db.persistChanges(discourse.graph)

    broadcastConnect(uuid, subGoal)
    Ok(Json.toJson(subGoal))
  }

  def connectProblem(uuid: String) = Action { request =>
    val json = request.body.asJson.get
    val connect = json.as[ConnectRequest]

    val (discourse, Seq(goal: Goal, problem: Problem)) = discourseNodes(uuid, connect.uuid)
    discourse.add(Prevents.local(problem, goal))
    db.persistChanges(discourse.graph)

    broadcastConnect(uuid, problem)
    Ok(Json.toJson(problem))
  }

  def connectIdea(uuid: String) = Action { request =>
    val json = request.body.asJson.get
    val connect = json.as[ConnectRequest]

    val (discourse, Seq(goal: Goal, idea: Idea)) = discourseNodes(uuid, connect.uuid)
    discourse.add(Achieves.local(idea, goal))
    db.persistChanges(discourse.graph)

    broadcastConnect(uuid, idea)
    Ok(Json.toJson(idea))
  }

  def disconnectGoal(uuid: String, uuidGoal: String) = Action {
    disconnect(uuidGoal, SubGoal.relationType, uuid)
    broadcastDisconnect(uuid, uuidGoal, "GOAL")
    Ok(JsObject(Seq()))
  }

  def disconnectProblem(uuid: String, uuidProblem: String) = Action {
    disconnect(uuidProblem, Prevents.relationType, uuid)
    broadcastDisconnect(uuid, uuidProblem, "PROBLEM")
    Ok(JsObject(Seq()))
  }

  def disconnectIdea(uuid: String, uuidIdea: String) = Action {
    disconnect(uuidIdea, List(Achieves.startRelationType, Achieves.endRelationType), uuid)
    broadcastDisconnect(uuid, uuidIdea, "IDEA")
    Ok(JsObject(Seq()))
  }
}
