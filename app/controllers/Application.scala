package controllers

import play.api.mvc._
import macros.hello

@hello
object Test extends App 

object Application extends Controller {
  def index(any: String) = Action {
    println(Test.hello)
    Ok(views.html.index())
  }
}
