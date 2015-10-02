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

    // When Neo4j throws an error because the regexp is incorrect, return an empty Discourse instead: Try(...).getOrElse(Discourse.empty)
    val discourse = Try(


    {
      val (query, params) = if(tags.isEmpty) {
        val startPostMatch = if(startPost) {
          s"match ${ nodeDef.toQuery }<-[:`${ SchemaTags.endRelationType }`]-(:`${ SchemaTags.label }`)<-[:`${ SchemaTags.startRelationType }`]-(:`${ Scope.label }`)"
        } else {
          ""
        }

        val condition = if(termMatcher.isEmpty) "" else s"where ${ termMatcher }"
        val query = s"""
          match ${ nodeDef.toQuery }
          $startPostMatch
          $condition
          return $returnStatement
          """
        val params = titleRegex.map(t => Map("term" -> t)).getOrElse(Map.empty) ++ nodeDef.parameterMap
        (query, params)
      } else {

        val condition = if(termMatcher.isEmpty) "" else s"where ${ termMatcher }"


        if(tagOr) {
          println("tagor")
          //TODO: classification need to be matched on the outgoing connects relation of the post
          val inheritTagDef = FactoryNodeDef(Scope)
          val tagDef = FactoryNodeDef(Scope)
          val tagDefinition = s"${ inheritTagDef.toQuery }-[:`${ Inherits.relationType }`*0..10]->${ tagDef.toQuery }"
          val inheritTagQuery =
            s"""
            match ${ tagDefinition }
            where (${ tagDef.name }.uuid in {tagUuids})
            with distinct ${ inheritTagDef.name }
            """

          val relationDef = RelationDef(inheritTagDef, SchemaTags, nodeDef)
          val tagOrMatch = s"""match ${ relationDef.toQuery(false, true) }"""


          val query =
            s"""
          $inheritTagQuery
          match ${ nodeDef.toQuery }
          $tagOrMatch
          $condition
          return $returnStatement"""
          val params = titleRegex.map(t => Map("term" -> t)).getOrElse(Map.empty) ++ Map("tagUuids" -> tags) ++ relationDef.parameterMap ++ tagDef.parameterMap ++ nodeDef.parameterMap
          (query, params)
        } else {
          //TODO: classification need to be matched on the outgoing connects relation of the post
          val tagDefs = tags.map(uuid => FactoryUuidNodeDef(Scope, uuid))
          val inheritTagDefs = tags.map(_ => FactoryNodeDef(Scope))
          val tagDefinitions = (tagDefs zip inheritTagDefs).map { case (t, i) => s"${ i.toQuery }-[:`${ Inherits.relationType }`*0..10]->${ t.toQuery }" }.mkString(",")
          val separateInheritTagQuery = s"""
          match ${ tagDefinitions }
          with distinct ${ inheritTagDefs.map(_.name).mkString(",") }
          """
          val relationDefs = inheritTagDefs.map(tagDef => RelationDef(tagDef, SchemaTags, nodeDef))
          val tagAndMatch = s"""match ${ relationDefs.map(_.toQuery(false)).mkString(",") }"""

          val query = s"""
          $separateInheritTagQuery

          match ${ nodeDef.toQuery }
          $tagAndMatch
          $condition
          return $returnStatement
          """

          val params = titleRegex.map(t => Map("term" -> t)).getOrElse(Map.empty) ++ Map("tagUuids" -> tags) ++ relationDefs.flatMap(_.parameterMap) ++ tagDefs.flatMap(_.parameterMap) ++ nodeDef.parameterMap
          (query, params)
        }
      }
      // println(query)
      // println(params)
      Discourse(db.queryGraph(query, params))
    }).getOrElse(Discourse.empty)

    Ok(Json.toJson(
      // we only add attached tags to the result when searching for posts
      if(labelOpt.contains(Post.label.name))
        TaggedTaggable.shapeResponse(discourse.posts)
      else
        discourse.exposedNodes
    ))
  }
}
