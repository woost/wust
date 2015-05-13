package controllers

import formatters.json.GraphFormat._
import modules.db.Database._
import play.api.libs.json._
import play.api.mvc.{Action, Controller}

object Graphs extends Controller {
  def show() = Action {
    val discourse = wholeDiscourseGraph
    Ok(Json.toJson(discourse))
  }
}
