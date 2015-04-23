package controllers

import modules.requests._
import play.api.libs.json._
import play.api.mvc.Action

import renesca._
import renesca.parameter.implicits._
import modules.json.GraphFormat._
import model.WustSchema._

object Ideas extends ContentNodesController[Idea] {
  override def factory = Idea
  override def apiname = "ideas"
  override def decodeRequest(jsValue: JsValue) = jsValue.as[IdeaAddRequest]

  def index() = Action {
    Ok(Json.toJson(wholeDiscourseGraph.ideas))
  }

  def showGoals(uuid: String) = Action {
    val query = Query(s"match (:${ Idea.label } {uuid: {uuid}})-[:${ Achieves.startRelationType }]->(:${ Achieves.label })-[:${ Achieves.endRelationType }]->(goal :${ Goal.label }) return goal", Map("uuid" -> uuid))
    Ok(Json.toJson(db.queryGraph(query).nodes.map(Goal.create)))
  }

  def showProblems(uuid: String) = Action {
    val query = Query(s"match (:${ Idea.label } {uuid: {uuid}})-[:${ Solves.startRelationType }]->(:${ Solves.label })-[:${ Solves.endRelationType }]->(problem :${ Problem.label }) return problem", Map("uuid" -> uuid))
    Ok(Json.toJson(db.queryGraph(query).nodes.map(Problem.create)))
  }

  def showIdeas(uuid: String) = Action {
    val query = Query(s"match (:${ Idea.label } {uuid: {uuid}})<-[:${ SubIdea.relationType }]-(idea :${ Idea.label }) return idea", Map("uuid" -> uuid))
    Ok(Json.toJson(db.queryGraph(query).nodes.map(Idea.create)))
  }

  def connectGoal(uuid: String) = Action(parse.json) { request =>
    val connect = request.body.as[ConnectRequest]

    val (_, goal) = connectNodes(uuid, connect.uuid, Achieves)
    broadcastConnect(uuid, goal)
    Ok(Json.toJson(goal))
  }

  def connectProblem(uuid: String) = Action(parse.json) { request =>
    val connect = request.body.as[ConnectRequest]

    val (_, problem) = connectNodes(uuid, connect.uuid, Solves)
    broadcastConnect(uuid, problem)
    Ok(Json.toJson(problem))
  }

  def connectIdea(uuid: String) = Action(parse.json) { request =>
    val connect = request.body.as[ConnectRequest]

    val (subIdea, _) = connectNodes(connect.uuid, uuid, SubIdea)
    broadcastConnect(uuid, subIdea)
    Ok(Json.toJson(subIdea))
  }

  def disconnectGoal(uuid: String, uuidGoal: String) = Action {
    disconnectNodes(uuid, List(Achieves.startRelationType, Achieves.endRelationType), uuidGoal)
    broadcastDisconnect(uuid, uuidGoal, "GOAL")
    Ok(JsObject(Seq()))
  }

  def disconnectProblem(uuid: String, uuidProblem: String) = Action {
    disconnectNodes(uuid, List(Solves.startRelationType, Solves.endRelationType), uuidProblem)
    broadcastDisconnect(uuid, uuidProblem, "PROBLEM")
    Ok(JsObject(Seq()))
  }

  def disconnectIdea(uuid: String, uuidIdea: String) = Action {
    disconnectNodes(uuid, SubIdea.relationType, uuidIdea)
    broadcastDisconnect(uuid, uuidIdea, "IDEA")
    Ok(JsObject(Seq()))
  }
}
