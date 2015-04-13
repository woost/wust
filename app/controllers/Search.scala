package controllers

import modules.json.GraphFormat._
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}
import renesca.Query
import renesca.graph.Label
import renesca.parameter.implicits._
import model.WustSchema._

object Search extends Controller with DatabaseController {
  def index(label: Option[String], title: Option[String]) = Action {
    val titleRegex = title match {
      case Some(title) => "(?i).*" + title.replace(" ", ". *") + ".*"
      case None        => ""
    }
    val nodeMatch = label match {
      case Some(label) => s"n:`${ Label(label) }`"
      case None        => "n"
    }

    val discourse = Discourse(db.queryGraph(Query(s"match ($nodeMatch) where n.title =~ {term} return n limit 15", Map("term" -> titleRegex))))
    Ok(Json.toJson(discourse.contentNodes))
  }
}
