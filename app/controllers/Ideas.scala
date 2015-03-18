package controllers

import modules.requests.{GoalAddRequest, IdeaAddRequest, NodeAddRequest, ProblemAddRequest}
import play.api.libs.json._
import play.api.mvc.Action
import play.api.mvc.Controller

import renesca._
import renesca.parameter.implicits._
import modules.json.GraphFormat._
import model._

object Ideas extends Controller with ContentNodesController[Idea] {
  override def factory = model.Idea
  override def apiname = "ideas"
  override def decodeRequest(jsValue: JsValue) = jsValue.as[IdeaAddRequest]

  def index() = Action {
    Ok(Json.toJson(wholeDiscourseGraph.ideas))
  }

  def showGoals(uuid: String) = Action {
    val query = Query(s"match (:${ Idea.label } {uuid: {uuid}})-[:${ IdeaToReaches.relationType }]->(:${ Reaches.label })-[:${ ReachesToGoal.relationType }]->(goal :${ Goal.label }) return goal", Map("uuid" -> uuid))
    Ok(Json.toJson(db.queryGraph(query).nodes.map(Goal.create)))
  }

  def showProblems(uuid: String) = Action {
    val query = Query(s"match (:${ Idea.label } {uuid: {uuid}})-[:${ IdeaToSolves.relationType }]->(:${ Solves.label })-[:${ SolvesToProblem.relationType }]->(problem :${ Problem.label }) return problem", Map("uuid" -> uuid))
    Ok(Json.toJson(db.queryGraph(query).nodes.map(Problem.create)))
  }

  def showIdeas(uuid: String) = Action {
    val query = Query(s"match (:${ Idea.label } {uuid: {uuid}})<-[:${ SubIdea.relationType }]-(idea :${ Idea.label }) return idea", Map("uuid" -> uuid))
    Ok(Json.toJson(db.queryGraph(query).nodes.map(Idea.create)))
  }

  def connectGoal(uuid: String, uuidGoal: String) = Action {
    val discourse = nodeDiscourseGraph(List(uuid, uuidGoal))
    val idea = discourse.ideas.find(p => p.uuid == uuid).get
    val goal = discourse.goals.find(p => p.uuid == uuidGoal).get
    val reaches = Reaches.local

    discourse.add(reaches)
    discourse.add(ReachesToGoal.local(reaches, goal))
    discourse.add(IdeaToReaches.local(idea, reaches))
    db.persistChanges(discourse.graph)

    broadcast(uuid, JsonChange("connect", Json.toJson(goal)))
    Ok(Json.toJson(goal))
  }

  def connectProblem(uuid: String, uuidProblem: String) = Action {
    val discourse = nodeDiscourseGraph(List(uuid, uuidProblem))
    val idea = discourse.ideas.find(p => p.uuid == uuid).get
    val problem = discourse.problems.find(p => p.uuid == uuidProblem).get
    val solves = Solves.local

    discourse.add(solves)
    discourse.add(SolvesToProblem.local(solves, problem))
    discourse.add(IdeaToSolves.local(idea, solves))
    db.persistChanges(discourse.graph)

    broadcast(uuid, JsonChange("connect", Json.toJson(problem)))
    Ok(Json.toJson(problem))
  }

  def connectIdea(uuid: String, uuidIdea: String) = Action {
    val discourse = nodeDiscourseGraph(List(uuid, uuidIdea))
    val idea = discourse.ideas.find(p => p.uuid == uuid).get
    val subIdea = discourse.ideas.find(p => p.uuid == uuidIdea).get

    discourse.add(SubIdea.local(subIdea, idea))
    db.persistChanges(discourse.graph)

    broadcast(uuid, JsonChange("connect", Json.toJson(idea)))
    Ok(Json.toJson(subIdea))
  }

  def disconnectGoal(uuid: String, uuidGoal: String) = Action {
    disconnect(uuid, List(IdeaToReaches.relationType, ReachesToGoal.relationType), uuidGoal)
    broadcastDisconnect(uuid,uuidGoal,"goal")
    Ok(JsObject(Seq()))
  }

  def disconnectProblem(uuid: String, uuidProblem: String) = Action {
    disconnect(uuid, List(IdeaToSolves.relationType, SolvesToProblem.relationType), uuidProblem)
    broadcastDisconnect(uuid,uuidProblem,"problem")
    Ok(JsObject(Seq()))
  }

  def disconnectIdea(uuid: String, uuidIdea: String) = Action {
    broadcastDisconnect(uuid,uuidIdea,"Idea")
    Ok(JsObject(Seq()))
  }
}
