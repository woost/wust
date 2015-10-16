package controllers

import controllers.api._
import formatters.json.ApiSchemaFormat._
import modules.requests._
import play.api.libs.json._
import play.api.mvc._
import play.api.Play.current
import common.ConfigString._

object Application extends Controller {
  //TODO create embedding type RequestSchema(ApiDefinition, Seq[NodeSchema])
  val apiDefinition = ApiDefinition("/api/v1", "/live/v1");
  val nodeSchemas = Seq(Posts, Scopes, Users, Connectables, ConnectsCtrl, ChangeRequests).map(_.nodeSchema)

  private val apiDefinitionJson = Json.toJson(apiDefinition);
  private val nodeSchemasJson = Json.toJson(JsArray(nodeSchemas.map(Json.toJson(_))));

  val scratchpadEnabled = "ui.scratchpad.enabled".configOrElse(true)

  def index(any: String) = Action {
    Ok(views.html.index(
      JsObject(Seq(
        ("api", apiDefinitionJson),
        ("models", nodeSchemasJson)
      )),
      JsObject(Seq(
        ("scratchpadEnabled", JsBoolean(scratchpadEnabled))
      ))
    ))
  }
}
