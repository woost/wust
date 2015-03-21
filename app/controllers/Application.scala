package controllers

import play.api.mvc._

object Application extends Controller {
  def index(any: String) = Action {
    Ok(views.html.index())
  }
}
