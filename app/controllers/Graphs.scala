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
  def show() = Action {
    val discourse = wholeDiscourseGraph
    Ok(Json.toJson(discourse))
  }
}
