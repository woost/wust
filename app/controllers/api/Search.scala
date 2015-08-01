package controllers.api

import modules.db.Database._
import modules.db.{LabelNodeDefinition,ConcreteFactoryNodeDefinition,RelationDefinition}
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}
import renesca.Query
import renesca.graph.Label
import renesca.parameter.implicits._
import model.WustSchema._
import formatters.json.ApiNodeFormat._

import scala.util.Try

object Search extends Controller {
  //TODO: yeah.
  def index(label: Option[String], title: Option[String], searchDescriptions:Option[Boolean], tags: List[String]) = Action {
    // white list, so only exposed nodes can be searched
    val labels = ContentNode.labels ++ label.map(Label(_))
    val nodeDef = LabelNodeDefinition(labels)

    val titleRegex = title.map(tit => "(?i).*" + tit.replace(" ", ".*") + ".*")
    val descrMatcher = if (searchDescriptions.getOrElse(false))
      Some(s"${nodeDef.name}.description =~ {term}")
    else
      None

    val titleMatcher = title.map(_ => s"${nodeDef.name}.title =~ {term}")
    val termMatcher = Seq(titleMatcher, descrMatcher).flatten.mkString(" or ")

    // When Neo4j throws an error because the regexp is incorrect, return an empty Discourse instead
    val discourse = Try(if (tags.isEmpty) {
      val condition = if (termMatcher.isEmpty) "" else s"where ${termMatcher}"

      Discourse(
        db.queryGraph(Query(s"""
          match ${nodeDef.toQuery}
          $condition
          return ${nodeDef.name} limit 15""",
          Map("term" -> titleRegex.getOrElse("")) ++ nodeDef.parameterMap))
      )
    } else {
      val tagDef = ConcreteFactoryNodeDefinition(Tag)
      val relationDef = RelationDefinition(tagDef, Categorizes, nodeDef)
      val condition = if (termMatcher.isEmpty) "" else s"and ${termMatcher}"

      Discourse(
        db.queryGraph(Query(s"""
          match ${relationDef.toQuery}
          where (${tagDef.name}.uuid in {tagUuids})
          $condition
          return ${nodeDef.name} limit 15""",
          Map("term" -> titleRegex.getOrElse(""), "tagUuids" -> tags)
            ++ relationDef.parameterMap
            ++ tagDef.parameterMap
            ++ nodeDef.parameterMap))
      )
    }).getOrElse(Discourse.empty)

    Ok(Json.toJson(discourse.contentNodes))
  }
}
