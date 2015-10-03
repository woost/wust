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
def index(labelOpt: Option[String], termOpt: Option[String], searchDescriptionsOpt: Option[Boolean], startPostOpt: Option[Boolean], tagsAll: List[String], tagsAny: List[String], tagsWithout: List[String], pageOpt: Option[Int], sizeOpt: Option[Int]) = Action {

    val searchDescriptions = searchDescriptionsOpt.getOrElse(false)
    val page = pageOpt.getOrElse(0)
    val startPost = startPostOpt.getOrElse(false)

    implicit val ctx = new QueryContext

    // white list, so only exposed nodes can be searched
    val labels = ExposedNode.labels ++ labelOpt.map(Label(_))
    val nodeDef = LabelNodeDef(labels)

    val termRegex = termOpt.flatMap { title =>
      if(title.trim.isEmpty)
        None
      else
        Some("(?i).*" + title.replace(" ", ".*") + ".*")
    }

    val preQueries = mutable.ArrayBuffer.empty[String]
    val postMatches = mutable.ArrayBuffer.empty[String]
    val postConditions = mutable.ArrayBuffer.empty[String]
    var params:ParameterMap = Map.empty[PropertyKey,ParameterValue]

    if(termRegex.isDefined) {
      val titleMatcher = s"${ nodeDef.name }.title =~ {term}"
      if( searchDescriptions )
        postConditions += s"$titleMatcher or ${ nodeDef.name }.description =~ {term}"
      else
        postConditions += titleMatcher

      params += ("term" -> termRegex.get)
    }

    if(startPost && tagsAll.isEmpty && tagsAny.isEmpty) {
      val tagsDef = RelationDef(FactoryNodeDef(Scope), SchemaTags, nodeDef)
      postMatches += s"match ${ tagsDef.toPattern }"
    }

    if(tagsAll.nonEmpty) {
      //TODO: classification need to be matched on the outgoing connects relation of the post
      val tagDefs = tagsAll.map(uuid => FactoryUuidNodeDef(Scope, uuid))
      val inheritTagDefs = tagsAll.map(_ => FactoryNodeDef(Scope))
      val tagDefinitions = (tagDefs zip inheritTagDefs).map { case (t, i) => s"${ i.toPattern }-[:`${ Inherits.relationType }`*0..10]->${ t.toPattern }" }.mkString(",")
      preQueries += s"""
      match ${ tagDefinitions }
      with distinct ${ inheritTagDefs.map(_.name).mkString(",") }
      """

      val relationDefs = inheritTagDefs.map(tagDef => RelationDef(tagDef, SchemaTags, nodeDef))
      postMatches += s"""match ${ relationDefs.map(_.toPattern(false)).mkString(",") }"""
    }

    if(tagsAny.nonEmpty) {
      //TODO: classification need to be matched on the outgoing connects relation of the post
      val inheritTagDef = FactoryNodeDef(Scope)
      val tagDef = FactoryNodeDef(Scope)
      val tagDefinition = s"${ inheritTagDef.toPattern }-[:`${ Inherits.relationType }`*0..10]->${ tagDef.toPattern }"
      preQueries +=
        s"""
        match ${ tagDefinition }
        where (${ tagDef.name }.uuid in {tagsAnyUuids})
        with distinct ${ inheritTagDef.name }
        """

        val relationDef = RelationDef(inheritTagDef, SchemaTags, nodeDef)
        postMatches += s"""match ${ relationDef.toPattern(false, true) }"""

        params += ("tagsAnyUuids" -> tagsAny)
    }

    if(tagsWithout.nonEmpty) {
      //TODO: classification need to be matched on the outgoing connects relation of the post
      val inheritTagDef = FactoryNodeDef(Scope)
      val tagDef = FactoryNodeDef(Scope)
      val tagDefinition = s"${ inheritTagDef.toPattern }-[:`${ Inherits.relationType }`*0..10]->${ tagDef.toPattern }"
      preQueries +=
        s"""
        match ${ tagDefinition }
        where (${ tagDef.name }.uuid in {tagsWithoutUuids})
        with distinct ${ inheritTagDef.name }
        """

        val relationDef = RelationDef(inheritTagDef, SchemaTags, nodeDef)
        postConditions += s"""not(${ relationDef.toPattern(false, true) })"""

        params += ("tagsWithoutUuids" -> tagsWithout)
    }

    val returnPostfix = sizeOpt.map { limit =>
      val skip = page * limit
      s"skip $skip limit $limit"
    }.getOrElse("")
    val returnStatement = s"${ nodeDef.name } order by ${ nodeDef.name }.timestamp desc $returnPostfix"

    val query = s"""
    ${preQueries.mkString("\n\n")}
    match ${ nodeDef.toPattern }
    ${postMatches.mkString("\n")}
    ${if(postConditions.nonEmpty) "where "+ postConditions.mkString("\nand ") else ""}
    return $returnStatement
    """

    val discourse = {
      // println("-"*30)
      // println(query)
      // println(params)
      // println("-"*30)
      // Discourse(db.queryGraph(query, params))
      // When Neo4j throws an error because the regexp is incorrect, return an empty Discourse instead
      Try(Discourse(db.queryGraph(query, ctx.params ++ params))).getOrElse(Discourse.empty)
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
