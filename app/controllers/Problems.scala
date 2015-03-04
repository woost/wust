package controllers

import play.api.libs.json._
import play.api.mvc.Action
import play.api.mvc.Controller

import renesca._
import renesca.graph._
import renesca.parameter.implicits._
import modules.json.GraphFormat._
import model._

object Problems extends Controller {
  val db = new DbService
  db.restService = new RestService("http://localhost:7474")

  def index() = Action {
    val graph = db.queryGraph("match (n) optional match (n)-[r]-() return n,r")
    val discourse = Discourse(graph)
    Ok(Json.toJson(discourse.problems.head))
  }

  def create() = Action { request =>
    val json = request.body.asJson.get
    val problem = json.as[DiscourseNode]
    val discourse = Discourse.empty
    discourse.add(problem)
    db.persistChanges(discourse.graph)
    Ok(Json.toJson(problem))
  }

}
