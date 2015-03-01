package controllers

import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc.Controller

object Graphs extends Controller {
  def index() = Action {
    Ok(Json.parse("""{"nodes":[{"id":0,"type":"problem","label":"Houston, we have a problem"},{"id":1,"type":"problem","label":"something relevant"},{"id":2,"type":"problem","label":"Something does not work"},{"id":4,"type":"problem","label":"Something works but is weird"}],"edges":[{"from":0,"to":0,"label":"causes"},{"from":1,"to":2,"label":"causes"}]}"""))
  }

  def show(id: String) = Action {
    Ok(Json.parse("""{"title":"Something works but is weird","type":"problem","ideas":[{"id":8,"type":"idea","label":"Deal with it!"},{"id":5,"type":"idea","label":"All software must die"}],"questions":[{"id":7,"type":"question","label":"Where is the problem?"}]}"""))
  }
}
