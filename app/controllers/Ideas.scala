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

  def createProblem(uuid: String) = Action { request =>
    val json = request.body.asJson.get
    val nodeAdd = json.as[ProblemAddRequest]

    val discourse = nodeDiscourseGraph(uuid)

    val idea = discourse.ideas.head
    val problem = Problem.local(nodeAdd.title)
    val solves = Solves.local

    discourse.add(solves)
    discourse.add(problem)

    discourse.add(SolvesToProblem.local(solves, problem))
    discourse.add(IdeaToSolves.local(idea, solves))

    db.persistChanges(discourse.graph)
    Ok(Json.toJson(problem))
  }

  def createGoal(uuid: String) = Action { request =>
    val json = request.body.asJson.get
    val nodeAdd = json.as[GoalAddRequest]

    val discourse = nodeDiscourseGraph(uuid)

    val idea = discourse.ideas.head
    val goal = Goal.local(nodeAdd.title)
    val reaches = Reaches.local

    discourse.add(reaches)
    discourse.add(goal)

    discourse.add(ReachesToGoal.local(reaches, goal))
    discourse.add(IdeaToReaches.local(idea, reaches))

    db.persistChanges(discourse.graph)
    Ok(Json.toJson(goal))
  }
}
