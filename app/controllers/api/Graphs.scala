package controllers.api

import formatters.json.GraphFormat._
import model.WustSchema._
import modules.db.Database._
import play.api.libs.json._
import play.api.mvc.{Action, Controller}

object Graphs extends Controller {
  def show() = Action {
    val discourse = Discourse(db.queryWholeGraph)
    Ok(Json.toJson(discourse))
  }
}
