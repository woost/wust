package controllers.api

import formatters.json.ExposedNodeFormat._
import model.WustSchema.{Tags => SchemaTags, _}
import modules.db.Database._
import modules.db._
import modules.db.helpers.TaggedTaggable
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}
import renesca.Query
import renesca.graph.Label
import renesca.parameter.implicits._
import renesca.parameter._
import collection.mutable

import scala.util.Try

object Search extends Controller {
  def index(pageOpt: Option[Int], sizeOpt: Option[Int], labelOpt: Option[String], titleOpt: Option[String], searchDescriptionsOpt: Option[Boolean], tags: List[String], tagOrOpt: Option[Boolean], startPostOpt: Option[Boolean]) = Action {

    val searchDescriptions = searchDescriptionsOpt.getOrElse(false)
    val page = pageOpt.getOrElse(0)
    val startPost = startPostOpt.getOrElse(false)
    val tagOr = tagOrOpt.getOrElse(false)

    implicit val ctx = new QueryContext

    // white list, so only exposed nodes can be searched
    val labels = ExposedNode.labels ++ labelOpt.map(Label(_))
    val nodeDef = LabelNodeDef(labels)

    val titleRegex = titleOpt.flatMap { title =>
      if(title.trim.isEmpty)
        None
      else
        Some("(?i).*" + title.replace(" ", ".*") + ".*")
    }

    val descrMatcher = titleRegex.flatMap { _ =>
      if(searchDescriptions)
        Some(s"${ nodeDef.name }.description =~ {term}")
      else
        None
    }

    val titleMatcher = titleRegex.map(_ => s"${ nodeDef.name }.title =~ {term}")
    val termMatcher = Seq(titleMatcher, descrMatcher).flatten.mkString(" or ")

    val returnPostfix = sizeOpt.map { limit =>
      val skip = page * limit
      s"skip $skip limit $limit"
    }.getOrElse("")
    val returnStatement = s"${ nodeDef.name } order by ${ nodeDef.name }.timestamp desc $returnPostfix"

    val discourse = {
      val preQueries = mutable.ArrayBuffer.empty[String]
      val postMatches = mutable.ArrayBuffer.empty[String]
      val postConditions = mutable.ArrayBuffer.empty[String]
      var params:ParameterMap = Map.empty[PropertyKey,ParameterValue]

      params ++= nodeDef.parameterMap

      if(titleRegex.isDefined)
        params += ("term" -> titleRegex.get)

      if(termMatcher.nonEmpty)
        postConditions +=  termMatcher

      if(startPost) {
        postMatches += s"match ${ nodeDef.toQuery }<-[:`${ SchemaTags.endRelationType }`]-(:`${ SchemaTags.label }`)<-[:`${ SchemaTags.startRelationType }`]-(:`${ Scope.label }`)"
      }

      if(tags.nonEmpty){
        if(tagOr) {
          //TODO: classification need to be matched on the outgoing connects relation of the post
          val inheritTagDef = FactoryNodeDef(Scope)
          val tagDef = FactoryNodeDef(Scope)
          val tagDefinition = s"${ inheritTagDef.toQuery }-[:`${ Inherits.relationType }`*0..10]->${ tagDef.toQuery }"
          preQueries +=
            s"""
            match ${ tagDefinition }
            where (${ tagDef.name }.uuid in {tagUuids})
            with distinct ${ inheritTagDef.name }
            """

            val relationDef = RelationDef(inheritTagDef, SchemaTags, nodeDef)
            postMatches += s"""match ${ relationDef.toQuery(false, true) }"""

            params += ("tagUuids" -> tags)
            params ++= relationDef.parameterMap
            params ++= tagDef.parameterMap
          } else {
            //TODO: classification need to be matched on the outgoing connects relation of the post
            val tagDefs = tags.map(uuid => FactoryUuidNodeDef(Scope, uuid))
            val inheritTagDefs = tags.map(_ => FactoryNodeDef(Scope))
            val tagDefinitions = (tagDefs zip inheritTagDefs).map { case (t, i) => s"${ i.toQuery }-[:`${ Inherits.relationType }`*0..10]->${ t.toQuery }" }.mkString(",")
            preQueries += s"""
            match ${ tagDefinitions }
            with distinct ${ inheritTagDefs.map(_.name).mkString(",") }
            """

            val relationDefs = inheritTagDefs.map(tagDef => RelationDef(tagDef, SchemaTags, nodeDef))
            postMatches += s"""match ${ relationDefs.map(_.toQuery(false)).mkString(",") }"""

            params += ("tagUuids" -> tags)
            params ++= relationDefs.flatMap(_.parameterMap)
            params ++= tagDefs.flatMap(_.parameterMap)
          }
      }

      val query = s"""
      ${preQueries.mkString(" ")}
      match ${ nodeDef.toQuery }
      ${postMatches.mkString(" ")}
      ${if(postConditions.nonEmpty) "where "+ postConditions.mkString(" and ") else ""}
      return $returnStatement
      """

      // println("-"*30)
      // println(query)
      // println(params)
      // println("-"*30)
      // Discourse(db.queryGraph(query, params))
      // When Neo4j throws an error because the regexp is incorrect, return an empty Discourse instead
      Try(Discourse(db.queryGraph(query, params))).getOrElse(Discourse.empty)
    }

    Ok(Json.toJson(
      // we only add attached tags to the result when searching for posts
      if(labelOpt.contains(Post.label.name))
        TaggedTaggable.shapeResponse(discourse.posts)
      else
        discourse.exposedNodes
      ))
  }
}
