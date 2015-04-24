package controllers

import play.api.mvc._
import play.api.libs.json._
import modules.json.GraphFormat._

object Application extends Controller {
  def index(any: String) = Action {
    Ok(views.html.index(JsObject(Seq(
      ("models", JsArray(Seq(
        Json.toJson(Goals.nodeSchema),
        Json.toJson(Problems.nodeSchema),
        Json.toJson(Ideas.nodeSchema)
      )))
    ))))
  }
}
