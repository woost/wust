package controllers.api

import modules.db.{FactoryUuidNodeDefinition, AnyUuidNodeDefinition}
import modules.db.Database._
import formatters.json.GraphFormat._
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}
import model.WustSchema.Post

object ConnectedComponents extends Controller {
  def show(uuid: String) = Action {
    val discourse = connectedComponent(FactoryUuidNodeDefinition(Post, uuid))
    if (discourse.nodes.isEmpty)
      NotFound(s"Cannot find node with uuid '$uuid'")
    else
      Ok(Json.toJson(discourse))
  }
}
