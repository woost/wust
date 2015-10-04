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
  def index(labelOpt: Option[String], termOpt: Option[String], searchDescriptionsOpt: Option[Boolean], startPostOpt: Option[Boolean], tagsAll: List[String], tagsAnyRaw: List[String], tagsWithout: List[String], classificationsAll: List[String], classificationsAnyRaw: List[String], classificationsWithout: List[String], pageOpt: Option[Int], sizeOpt: Option[Int]) = Action {

    val discourse = if(
      (tagsAll intersect tagsWithout).nonEmpty ||
      (classificationsAll intersect classificationsWithout).nonEmpty ||
      (tagsAnyRaw.size > 0 && tagsAnyRaw.toSet.subsetOf(tagsWithout.toSet)) ||
      (classificationsAnyRaw.size > 0 && classificationsAnyRaw.toSet.subsetOf(classificationsWithout.toSet))
    ) {
      Discourse.empty
    } else {
      //TODO: uniq tag and classification ids
      val searchDescriptions = searchDescriptionsOpt.getOrElse(false)
      val page = pageOpt.getOrElse(0)
      val startPost = startPostOpt.getOrElse(false)
      val tagsAny = (tagsAnyRaw diff tagsWithout) diff tagsAll
      val classificationsAny = (classificationsAnyRaw diff classificationsWithout) diff classificationsAll

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
      val lastQueries = mutable.ArrayBuffer.empty[String]
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
        val tagsDef = AnonRelationDef(FactoryNodeDef(Scope), SchemaTags, nodeDef)
        postMatches += s"match ${ tagsDef.toPattern }"
      }

      if(tagsAll.nonEmpty) {
        //TODO: classification need to be matched on the outgoing connects relation of the post
        val tagDefs = tagsAll.map(uuid => FactoryUuidNodeDef(Scope, uuid))
        val inheritTagDefs = tagsAll.map(_ => FactoryNodeDef(Scope))
        val tagDefinitions = (tagDefs zip inheritTagDefs).map { case (t, i) => s"${ i.toPattern }-[:`${ Inherits.relationType }`*0..10]->${ t.toPattern }" }.mkString(",")
        preQueries += s"""
        match ${ tagDefinitions }
        with distinct *
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
          with distinct *
          """

          val relationDef = AnonRelationDef(inheritTagDef, SchemaTags, nodeDef)
          postMatches += s"""match ${ relationDef.toPattern(false, false) }"""

          params += ("tagsAnyUuids" -> tagsAny)
      }

      if(classificationsAny.nonEmpty) {
        //TODO: classification need to be matched on the outgoing connects relation of the post
        val classificationDef = FactoryNodeDef(Classification)
        preQueries += s"""
        match ${classificationDef.toPattern}
        where ${classificationDef.name}.uuid in {classificationsAnyUuids}
        with *
        """

        val connectsDef = HyperNodeDef(nodeDef, Connects, FactoryNodeDef(Post))
        val classifiesConnectsDef = AnonRelationDef(classificationDef, Classifies, connectsDef)
        val tagsDef = HyperNodeDef(FactoryNodeDef(Scope), SchemaTags, nodeDef)
        val classifiesTagsDef = AnonRelationDef(classificationDef, Classifies, tagsDef)
        postMatches += s"""
        optional match ${connectsDef.toPattern(false, true)}
        optional match ${tagsDef.toPattern(true, false)}
        with *
        """
        postConditions += s"""(
          (${connectsDef.endName} is not null and ${ classifiesConnectsDef.toPattern(false) })
          or (${tagsDef.startName} is not null and ${ classifiesTagsDef.toPattern(false) })
        )"""

        params += ("classificationsAnyUuids" -> classificationsAny)
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
          with distinct *
          """

          val relationDef = RelationDef(inheritTagDef, SchemaTags, nodeDef)
          postMatches += s"""optional match ${ relationDef.toPattern(false, false) }"""
          lastQueries += s"""with ${nodeDef.name}, count(${relationDef.name}) as tagsWithoutCount
          where tagsWithoutCount = 0"""

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
      ${lastQueries.mkString("\n")}
      return $returnStatement
      """

      // println("-"*30)
      // println(query)
      // println(ctx.params ++ params)
      // def parameterToString(p:Any):String = p match {
      //   // case array:List[String] => s"[${array.map(parameterToString).mkString(",")}]"
      //   case ArrayParameterValue(array) => s"[${array.map(parameterToString).mkString(",")}]"
      //   case StringPropertyValue(str) => s""""${str}""""
      //   case v => v.toString
      // }
      // println((ctx.params ++ params).map{case(k,v) => (s"\\{$k\\}", parameterToString(v))}.foldLeft(query){case (z, (s,r)) => z.replaceAll(s, r)})
      // println("-"*30)
      // Discourse(db.queryGraph(query, ctx.params ++ params))
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
