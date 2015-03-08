package controllers

import modules.requests.{GoalAddRequest, IdeaAddRequest, NodeAddRequest, ProblemAddRequest}
import play.api.libs.json._
import play.api.mvc.Action
import play.api.mvc.Controller

import renesca._
import renesca.parameter.implicits._
import modules.json.GraphFormat._
import model._

object Problems extends Controller with ContentNodesController[Problem] {
  override def factory = model.Problem
  override def decodeRequest(jsValue: JsValue) = jsValue.as[ProblemAddRequest]

  def index() = Action {
    Ok(Json.toJson(wholeDiscourseGraph.problems))
  }

  def showGoals(uuid: String) = Action {
    val query = Query(s"match (:${ Problem.label } {uuid: {uuid}})-[:${ Prevents.relationType }]->(goal :${ Goal.label }) return goal", Map("uuid" -> uuid))
    Ok(Json.toJson(db.queryGraph(query).nodes.map(Goal.create)))
  }

  def showIdeas(uuid: String) = Action {
    val query = Query(s"match (:${ Problem.label } {uuid: {uuid}})<-[:${ SolvesToProblem.relationType }]-(:${ Solves.label })<-[:${ IdeaToSolves.relationType }]-(idea :${ Idea.label }) return idea", Map("uuid" -> uuid))
    Ok(Json.toJson(db.queryGraph(query).nodes.map(Idea.create)))
  }

  def showProblems(uuid: String) = Action {
    val query = Query(s"match (:${ Problem.label } {uuid: {uuid}})-[:${ Causes.relationType }]-(problem :${ Problem.label }) return problem", Map("uuid" -> uuid))
    Ok(Json.toJson(db.queryGraph(query).nodes.map(Problem.create)))
  }

  def createGoal(uuid: String) = Action { request =>
    val json = request.body.asJson.get
    val nodeAdd = json.as[GoalAddRequest]

    val discourse = nodeDiscourseGraph(uuid)

    val problem = discourse.problems.head
    val goal = Goal.local(nodeAdd.title)

    discourse.add(goal)

    discourse.add(Prevents.local(problem, goal))

    db.persistChanges(discourse.graph)
    Ok(Json.toJson(goal))
  }

  def createProblem(uuid: String) = Action { request =>
    val json = request.body.asJson.get
    val nodeAdd = json.as[ProblemAddRequest]

    val discourse = nodeDiscourseGraph(uuid)

    val problem = discourse.problems.head
    val consequence = Problem.local(nodeAdd.title)

    discourse.add(consequence)

    discourse.add(Causes.local(problem, consequence))

    db.persistChanges(discourse.graph)
    Ok(Json.toJson(consequence))
  }

  def createIdea(uuid: String) = Action { request =>
    val json = request.body.asJson.get
    val nodeAdd = json.as[IdeaAddRequest]

    val discourse = nodeDiscourseGraph(uuid)

    val problem = discourse.problems.head
    val idea = Idea.local(nodeAdd.title)
    val solves = Solves.local

    discourse.add(solves)
    discourse.add(idea)

    discourse.add(SolvesToProblem.local(solves, problem))
    discourse.add(IdeaToSolves.local(idea, solves))

    db.persistChanges(discourse.graph)
    Ok(Json.toJson(idea))
  }
}

