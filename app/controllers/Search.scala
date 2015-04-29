package controllers

import modules.json.GraphFormat._
import modules.db.Database._
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}
import renesca.Query
import renesca.graph.Label
import renesca.parameter.implicits._
import model.WustSchema._

import scala.util.Try

object Search extends Controller {
  def index(label: Option[String], title: Option[String]) = Action {
    val titleRegex = title match {
      case Some(title) => "(?i).*" + title.replace(" ", ". *") + ".*"
      case None        => ""
    }
    val nodeMatch = label match {
      case Some(label) => s"n:`${ Label(label) }`"
      case None        => "n"
    }

    // When Neo4j throws an error because the regexp is incorrect, return an empty Discourse instead
    val discourse = Try(
      Discourse(
        db.queryGraph(Query(s"match ($nodeMatch) where n.title =~ {term} return n limit 15", Map("term" -> titleRegex))))
    ).getOrElse(Discourse.empty)
    Ok(Json.toJson(discourse.contentNodes))
  }
}
