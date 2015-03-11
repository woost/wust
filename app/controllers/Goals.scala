package controllers

import model._
import modules.json.GraphFormat._
import modules.requests.{GoalAddRequest, ProblemAddRequest, IdeaAddRequest}
import play.api.libs.json._
import play.api.mvc.{Action, Controller}
import renesca._
import renesca.parameter.implicits._

object Goals extends Controller with ContentNodesController[Goal] {
  override def factory = model.Goal
  override def decodeRequest(jsValue: JsValue) = jsValue.as[GoalAddRequest]

  def index() = Action {
    Ok(Json.toJson(wholeDiscourseGraph.goals))
  }

  def showIdeas(uuid: String) = Action {
    val query = Query(s"match (:${ Goal.label } {uuid: {uuid}})<-[:${ ReachesToGoal.relationType }]-(:${ Reaches.label })<-[:${ IdeaToReaches.relationType }]-(idea :${ Idea.label }) return idea", Map("uuid" -> uuid))
    Ok(Json.toJson(db.queryGraph(query).nodes.map(Goal.create)))
  }

  def showProblems(uuid: String) = Action {
    val query = Query(s"match (:${ Goal.label } {uuid: {uuid}})<-[:${ Prevents.relationType }]-(problem :${ Problem.label }) return problem", Map("uuid" -> uuid))
    Ok(Json.toJson(db.queryGraph(query).nodes.map(Problem.create)))
  }

  def createProblem(uuid: String) = Action { request =>
    val json = request.body.asJson.get
    val nodeAdd = json.as[ProblemAddRequest]

    val discourse = nodeDiscourseGraph(uuid)

    val goal = discourse.goals.head
    val problem = Problem.local(nodeAdd.title)

    discourse.add(problem)

    discourse.add(Prevents.local(problem, goal))

    db.persistChanges(discourse.graph)
    Ok(Json.toJson(problem))
  }

  def createIdea(uuid: String) = Action { request =>
    val json = request.body.asJson.get
    val nodeAdd = json.as[IdeaAddRequest]

    val discourse = nodeDiscourseGraph(uuid)

    val goal = discourse.goals.head
    val idea = Idea.local(nodeAdd.title)
    val reaches = Reaches.local

    discourse.add(reaches)
    discourse.add(idea)

    discourse.add(ReachesToGoal.local(reaches, goal))
    discourse.add(IdeaToReaches.local(idea, reaches))

    db.persistChanges(discourse.graph)
    Ok(Json.toJson(idea))
  }

  def removeProblem(uuid: String, uuidProblem: String) = Action {
    val discourse = relationDiscourseGraph(uuidProblem, Prevents.relationType, uuid)
    discourse.graph.relations.clear()
    db.persistChanges(discourse.graph)
    Ok(JsObject(Seq()))
  }
}
