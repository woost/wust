package controllers.api

import modules.db.Database._
import play.api.libs.json._
import play.api.mvc.{Action, Controller}
import model.WustSchema._
import formatters.json.GraphFormat._

object Graphs extends Controller {
  def show() = Action {
    val discourse = Discourse(db.queryWholeGraph)
    Ok(Json.toJson(discourse))
  }
}
