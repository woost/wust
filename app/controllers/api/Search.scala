package controllers.api

import modules.db.Database._
import modules.db._
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
  def index(page: Option[Int], label: Option[String], title: Option[String], searchDescriptions:Option[Boolean], tags: List[String], tagOr: Option[Boolean]) = Action {
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

    val limit = 15
    val skip = page.getOrElse(0) * limit;
    val returnStatement = s"return ${ nodeDef.name } order by ${ nodeDef.name }.title skip $skip limit $limit"

    // When Neo4j throws an error because the regexp is incorrect, return an empty Discourse instead
    val discourse = Try(if (tags.isEmpty) {
      val condition = if (termMatcher.isEmpty) "" else s"where ${termMatcher}"

      Discourse(db.queryGraph(Query(
        s"""match ${nodeDef.toQuery}
        $condition
        $returnStatement""",
        Map("term" -> titleRegex.getOrElse("")) ++ nodeDef.parameterMap
      )))
    } else {
      if (tagOr.getOrElse(false)) {
        val tagDef = ConcreteFactoryNodeDefinition(Tag)
        val relationDef = RelationDefinition(tagDef, Categorizes, nodeDef)
        val condition = if (termMatcher.isEmpty) "" else s"and ${termMatcher}"

        Discourse(db.queryGraph(Query(
          s"""match ${relationDef.toQuery}
          where (${tagDef.name}.uuid in {tagUuids})
          $condition
          $returnStatement""",
          Map("term" -> titleRegex.getOrElse(""), "tagUuids" -> tags)
          ++ relationDef.parameterMap
          ++ tagDef.parameterMap
          ++ nodeDef.parameterMap
        )))
      } else {
        val tagDefs = tags.map(uuid => FactoryUuidNodeDefinition(Tag, uuid))
        val relationDefs = tagDefs.map(tagDef => RelationDefinition(tagDef, Categorizes, nodeDef))
        val condition = if (termMatcher.isEmpty) "" else s"where ${termMatcher}"

        Discourse(db.queryGraph(Query(
          s"""match ${nodeDef.toQuery}, ${tagDefs.map(_.toQuery).mkString(",")}, ${relationDefs.map(_.toQuery(false)).mkString(",")}
          $condition
          $returnStatement""",
          Map("term" -> titleRegex.getOrElse(""), "tagUuids" -> tags)
          ++ relationDefs.flatMap(_.parameterMap)
          ++ tagDefs.flatMap(_.parameterMap)
          ++ nodeDef.parameterMap
        )))
      }
    }).getOrElse(Discourse.empty)

    Ok(Json.toJson(discourse.contentNodes))
  }
}
