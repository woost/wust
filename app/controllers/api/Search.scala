package controllers.api

import modules.db.Database._
import modules.db.LabelNodeDefinition
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}
import renesca.Query
import renesca.graph.Label
import renesca.parameter.implicits._
import model.WustSchema._
import formatters.json.ApiNodeFormat._

import scala.util.Try

object Search extends Controller {
  def index(label: Option[String], title: Option[String], searchDescriptions:Option[Boolean]) = Action {
    val titleRegex = title match {
      case Some(title) => "(?i).*" + title.replace(" ", ".*") + ".*"
      case None        => ""
    }

    // white list, so only exposed nodes can be searched
    val labels = ContentNode.labels ++ label.map(Label(_))
    val nodeDef = LabelNodeDefinition(labels)

    val withDescr = searchDescriptions.getOrElse(false)

    // When Neo4j throws an error because the regexp is incorrect, return an empty Discourse instead
    val discourse = Try(
      Discourse(
        db.queryGraph(Query(s"""
          match ${nodeDef.toQuery}
          where ${nodeDef.name}.title =~ {term} ${if(withDescr) s"or ${nodeDef.name}.description =~ {term}" else ""}
          return ${nodeDef.name} limit 15""",
          Map("term" -> titleRegex) ++ nodeDef.parameterMap)))
    ).getOrElse(Discourse.empty)
    Ok(Json.toJson(discourse.contentNodes))
  }
}
