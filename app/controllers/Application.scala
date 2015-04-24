package controllers

import play.api.mvc._
import play.api.libs.json._
import modules.requests._
import modules.json.GraphFormat._
import model.WustSchema._

object Application extends Controller {
  val nodeSchemas = Seq(Goals.nodeSchema, Problems.nodeSchema, Ideas.nodeSchema)

  def index(any: String) = Action {
    Ok(views.html.index(JsObject(Seq(
      ("models", JsArray(nodeSchemas.map(Json.toJson(_)))
    )))))
  }
}
