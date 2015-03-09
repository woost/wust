package controllers

import play.api.libs.json._
import play.api.mvc.Action
import play.api.mvc.Controller

import renesca._
import renesca.graph._
import renesca.parameter.implicits._
import modules.json.GraphFormat._
import model._

object Graphs extends Controller with DatabaseController {
  def index() = Action {
    val discourse = wholeDiscourseGraph
    Ok(Json.toJson(discourse))
  }

  def remove(uuid: String) = Action {
    val discourse = nodeDiscourseGraph(uuid)
    discourse.graph.nodes.clear
    db.persistChanges(discourse.graph)
    Ok(JsObject(Seq()))
  }

  def show(uuid: String) = Action {
    val discourse = nodeDiscourseGraph(uuid)
    val nodes = discourse.nodes
    if(nodes.isEmpty) {
      BadRequest
    } else {
      Ok(Json.toJson(nodes.head))
    }
  }
}
