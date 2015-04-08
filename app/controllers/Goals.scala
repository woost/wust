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
    val query = Query(s"match (:${ Goal.label } {uuid: {uuid}})<-[:${ ReachesToGoal.relationType }]-(:${ Reaches.label })<-[:${ IdeaToReaches.relationType }]-(idea :${ Idea.label }) return idea", Map("uuid" -> uuid))
    Ok(Json.toJson(db.queryGraph(query).nodes.map(Idea.create)))
  }

  def connectGoal(uuid: String) = Action { request =>
    val json = request.body.asJson.get
    val connect = json.as[ConnectRequest]

    val discourse = nodeDiscourseGraph(List(uuid, connect.uuid))
    val goal = discourse.goals.find(p => p.uuid == uuid).get
    val subGoal = discourse.goals.find(p => p.uuid == connect.uuid).get

    discourse.add(SubGoal.local(subGoal, goal))
    db.persistChanges(discourse.graph)

    broadcastConnect(uuid, subGoal)
    Ok(Json.toJson(subGoal))
  }

  def connectProblem(uuid: String) = Action { request =>
    val json = request.body.asJson.get
    val connect = json.as[ConnectRequest]

    val discourse = nodeDiscourseGraph(List(uuid, connect.uuid))
    val goal = discourse.goals.find(p => p.uuid == uuid).get
    val problem = discourse.problems.find(p => p.uuid == connect.uuid).get

    discourse.add(Prevents.local(problem, goal))
    db.persistChanges(discourse.graph)

    broadcastConnect(uuid, problem)
    Ok(Json.toJson(problem))
  }

  def connectIdea(uuid: String) = Action { request =>
    val json = request.body.asJson.get
    val connect = json.as[ConnectRequest]

    val discourse = nodeDiscourseGraph(List(uuid, connect.uuid))
    val goal = discourse.goals.find(p => p.uuid == uuid).get
    val idea = discourse.ideas.find(p => p.uuid == connect.uuid).get
    val reaches = Reaches.local

    discourse.add(reaches)
    discourse.add(ReachesToGoal.local(reaches, goal))
    discourse.add(IdeaToReaches.local(idea, reaches))
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
    disconnect(uuidIdea, List(IdeaToReaches.relationType, ReachesToGoal.relationType), uuid)
    broadcastDisconnect(uuid, uuidIdea, "IDEA")
    Ok(JsObject(Seq()))
  }
}
