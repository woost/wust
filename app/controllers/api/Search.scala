package controllers.api

import modules.db.Database._
import modules.db._
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}
import renesca.Query
import renesca.graph.Label
import renesca.parameter.implicits._
import model.WustSchema.{Tags => SchemaTags, _}
import formatters.json.ApiNodeFormat._
import modules.db.access.custom.TaggedTaggable

import scala.util.Try

object Search extends TaggedTaggable[Post] with Controller {
  def index(page: Option[Int], size: Option[Int], label: Option[String], title: Option[String], searchDescriptions:Option[Boolean], tags: List[String], tagOr: Option[Boolean], startPost: Option[Boolean]) = Action {
    // white list, so only exposed nodes can be searched
    val labels = ExposedNode.labels ++ label.map(Label(_))
    val nodeDef = LabelNodeDefinition(labels)

    val titleRegex = title.flatMap { tit =>
      if (tit.trim.isEmpty)
        None
      else
        Some("(?i).*" + tit.replace(" ", ".*") + ".*")
    }

    val startPostMatchPostfix = if (startPost.getOrElse(false)) {
      //WARUM GIBT DAS KEINEN ERROR IN NEO$J CYPHER
      s"<-[:`${SchemaTags.endRelationType}`]-(:`${SchemaTags.label}`)<-[:`${SchemaTags.startRelationType}`]-(:`${Scope.label}`)"
    } else {
      ""
    }

    val descrMatcher = titleRegex.flatMap { _ =>
      if (searchDescriptions.getOrElse(false))
        Some(s"${nodeDef.name}.description =~ {term}")
      else
        None
    }

    val titleMatcher = titleRegex.map(_ => s"${nodeDef.name}.title =~ {term}")
    val termMatcher = Seq(titleMatcher, descrMatcher).flatten.mkString(" or ")

    val returnPostfix = size.map { limit =>
      val skip = page.getOrElse(0) * limit;
      s"skip $skip limit $limit"
    }.getOrElse("")
    val returnStatement = s"return ${ nodeDef.name } order by ${ nodeDef.name }.timestamp desc $returnPostfix"

    // When Neo4j throws an error because the regexp is incorrect, return an empty Discourse instead
    val discourse = Try(if (tags.isEmpty) {
      val condition = if (termMatcher.isEmpty) "" else s"where ${termMatcher}"

      Discourse(db.queryGraph(Query(
        s"""match ${nodeDef.toQuery}$startPostMatchPostfix
        $condition
        $returnStatement""",
        titleRegex.map(t => Map("term" -> t)).getOrElse(Map.empty) ++ nodeDef.parameterMap
      )))
    } else {
      if (tagOr.getOrElse(false)) {
        //TODO: classification need to be matched on the outgoing connects relation of the post
        val tagDef = ConcreteFactoryNodeDefinition(Scope)
        val inheritTagDef = ConcreteFactoryNodeDefinition(Scope)
        val relationDef = RelationDefinition(inheritTagDef, SchemaTags, nodeDef)
        val condition = if (termMatcher.isEmpty) "" else s"where ${termMatcher}"
        val tagDefinition = s"${inheritTagDef.toQuery}-[:INHERITSTOINHERITABLE|INHERITABLETOINHERITS*0..10]->${tagDef.toQuery}"

        Discourse(db.queryGraph(Query(
          s"""match ${tagDefinition} where (${tagDef.name}.uuid in {tagUuids})
          with distinct ${inheritTagDef.name} match ${relationDef.toQuery(false, true)}$startPostMatchPostfix
          $condition
          $returnStatement""",
          titleRegex.map(t => Map("term" -> t)).getOrElse(Map.empty) ++ Map("tagUuids" -> tags)
          ++ relationDef.parameterMap
          ++ tagDef.parameterMap
          ++ nodeDef.parameterMap
        )))
      } else {
        //TODO: classification need to be matched on the outgoing connects relation of the post
        val tagDefs = tags.map(uuid => FactoryUuidNodeDefinition(Scope, uuid))
        val inheritTagDefs = tags.map(_ => ConcreteFactoryNodeDefinition(Scope))
        val relationDefs = inheritTagDefs.map(tagDef => RelationDefinition(tagDef, SchemaTags, nodeDef))
        val condition = if (termMatcher.isEmpty) "" else s"where ${termMatcher}"
        val tagDefinitions = (tagDefs zip inheritTagDefs).map { case (t,i) => s"${i.toQuery}-[:INHERITSTOINHERITABLE|INHERITABLETOINHERITS*0..10]->${t.toQuery}" }.mkString(",")

        //TODO: distinct?
        Discourse(db.queryGraph(Query(
          s"""match ${nodeDef.toQuery}$startPostMatchPostfix, ${tagDefinitions}, ${relationDefs.map(_.toQuery(false)).mkString(",")}
          $condition
          $returnStatement""",
          titleRegex.map(t => Map("term" -> t)).getOrElse(Map.empty) ++ Map("tagUuids" -> tags)
          ++ relationDefs.flatMap(_.parameterMap)
          ++ tagDefs.flatMap(_.parameterMap)
          ++ nodeDef.parameterMap
        )))
      }
    }).getOrElse(Discourse.empty)

    Ok(Json.toJson(
      // we only add attached tags to the result when searching for posts
      if (label.contains(Post.label.name))
        shapeResponse(discourse.posts)
      else
        discourse.uuidNodes
    ))
  }
}
