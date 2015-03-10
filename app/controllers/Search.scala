package controllers

import model.Discourse
import modules.json.GraphFormat._
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}
import renesca.Query
import renesca.parameter.implicits._

object Search extends Controller with DatabaseController {
  def search(term: String, labelOpt:Option[String]) = Action {
    val regexTerm = ".*" + term.replace(" ", ". *") + ".*"
    val nodeMatch = labelOpt match {
      case Some(label) => s"n:$label"
      case None => "n"
    }
    val discourse = Discourse(db.queryGraph(Query(s"match ($nodeMatch) where n.title =~ {term} return n limit 15", Map("term" -> regexTerm))))

    Ok(Json.toJson(discourse.nodes))
  }

  def index(term: String) = search(term, None)
  def indexWithLabel(term: String, label: String) = search(term, Some(label))
}

