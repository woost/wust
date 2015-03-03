package controllers

import java.io.File

import play.api._
import play.api.Play.current
import play.api.mvc._

object Application extends Controller {
  def index(any: String) = Action {
    Ok(views.html.index())
  }
}
