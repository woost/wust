package controllers.api

import modules.db.AnyUuidNodeDefinition
import modules.db.Database._
import formatters.json.GraphFormat._
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}

object ConnectedComponents extends Controller {
  def show(uuid: String) = Action {
    val discourse = connectedComponent(AnyUuidNodeDefinition(uuid))
    Ok(Json.toJson(discourse))
  }
}
