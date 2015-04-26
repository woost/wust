package controllers

import play.api.mvc._
import play.api.libs.json._
import modules.requests._
import modules.json.GraphFormat._
import model.WustSchema._

object Application extends Controller {
  //TODO create embedding type RequestSchema(ApiDefinition, Seq[NodeSchema])
  val apiDefinition = ApiDefinition("/api/v1", "/live/v1");
  val nodeSchemas = Seq(Goals.nodeSchema, Problems.nodeSchema, Ideas.nodeSchema, ProArguments.nodeSchema, ConArguments.nodeSchema)

  def index(any: String) = Action {
    Ok(views.html.index(JsObject(Seq(
      ("api", Json.toJson(apiDefinition)),
      ("models", JsArray(nodeSchemas.map(Json.toJson(_))))
    ))))
  }
}
