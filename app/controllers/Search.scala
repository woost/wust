package controllers

import model.Discourse
import modules.json.GraphFormat._
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}
import renesca.Query
import renesca.parameter.implicits._

object Search extends Controller with DatabaseController {
  def index(term: String) = Action {
    val regexTerm = ".*" + term.replace(" ", ".*") + ".*"
    val discourse = Discourse(db.queryGraph(Query("match (n) where n.title =~ {term} return n limit 15", Map("term" -> regexTerm))))

    Ok(Json.toJson(discourse.nodes))
  }
}

