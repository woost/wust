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
  def index(labelOpt: Option[String], termOpt: Option[String], searchDescriptionsOpt: Option[Boolean], startPostOpt: Option[Boolean], contextsAll: List[String], contextsAnyRaw: List[String], contextsWithout: List[String], classificationsAll: List[String], classificationsAnyRaw: List[String], classificationsWithout: List[String], pageOpt: Option[Int], sizeOpt: Option[Int]) = Action {

    val discourse = if(
      (contextsAll intersect contextsWithout).nonEmpty ||
      (classificationsAll intersect classificationsWithout).nonEmpty ||
      (contextsAnyRaw.size > 0 && contextsAnyRaw.toSet.subsetOf(contextsWithout.toSet)) ||
      (classificationsAnyRaw.size > 0 && classificationsAnyRaw.toSet.subsetOf(classificationsWithout.toSet))
    ) {
      Discourse.empty
    } else {
      //TODO: uniq tag and classification ids
      val searchDescriptions = searchDescriptionsOpt.getOrElse(false)
      val page = pageOpt.getOrElse(0)
      val startPost = startPostOpt.getOrElse(false)
      val contextsAny = (contextsAnyRaw diff contextsWithout) diff contextsAll
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
      val lastWiths = mutable.ArrayBuffer.empty[String]
      val lastConditions = mutable.ArrayBuffer.empty[String]
      var lastDistinct = false
      var params:ParameterMap = Map.empty[PropertyKey,ParameterValue]

      if(termRegex.isDefined) {
        val titleMatcher = s"${ nodeDef.name }.title =~ {term}"
        if( searchDescriptions )
          postConditions += s"$titleMatcher or ${ nodeDef.name }.description =~ {term}"
        else
          postConditions += titleMatcher

        params += ("term" -> termRegex.get)
      }

      if(startPost && contextsAll.isEmpty && contextsAny.isEmpty) {
        val tagsDef = AnonRelationDef(FactoryNodeDef(Scope), SchemaTags, nodeDef)
        postMatches += s"match ${ tagsDef.toPattern }"
      }

      //TODO: FIXME: Wrong for classificationsAll + classificationsAny + contextsAny

      if(contextsAll.nonEmpty) {
        val contextDefs = contextsAll.map(uuid => FactoryUuidNodeDef(Scope, uuid))
        val inheritContextDefs = contextsAll.map(_ => FactoryNodeDef(Scope))
        val tagDefinitions = (contextDefs zip inheritContextDefs).map { case (t, i) => s"${ i.toPattern }-[:`${ Inherits.relationType }`*0..10]->${ t.toPattern }" }.mkString(",")
        preQueries += s"""
        match ${ tagDefinitions }
        with distinct *
        """

        val relationDefs = inheritContextDefs.map(tagDef => RelationDef(tagDef, SchemaTags, nodeDef))
        postMatches += s"""match ${ relationDefs.map(_.toPattern(false)).mkString(",") }"""
      }

      if(classificationsAll.nonEmpty) {
        val classificationDef = FactoryNodeDef(Classification)
        preQueries += s"""
        match ${classificationDef.toPattern}
        where ${classificationDef.name}.uuid in {classificationsAllUuids}
        with *
        """

        val connectsDef = HyperNodeDef(nodeDef, Connects, FactoryNodeDef(Post))
        val classifiesConnectsDef = AnonRelationDef(classificationDef, Classifies, connectsDef)
        val tagsDef = HyperNodeDef(FactoryNodeDef(Scope), SchemaTags, nodeDef)
        val classifiesTagsDef = AnonRelationDef(classificationDef, Classifies, tagsDef)
        postMatches += s"""
        optional match ${connectsDef.toPattern(false, true)}, ${ classifiesConnectsDef.toPattern(false) }
        optional match ${tagsDef.toPattern(true, false)}, ${ classifiesTagsDef.toPattern(false) }
        with *
        """

        lastWiths += s"""count(${connectsDef.name}) + count(${tagsDef.name}) as classificationsAllMatched"""
        lastConditions += s"""classificationsAllMatched >= {classificationsAllCount}"""

        params += ("classificationsAllUuids" -> classificationsAll)
        params += ("classificationsAllCount" -> classificationsAll.size)
      }

      if(contextsAny.nonEmpty) {
        val inheritTagDef = FactoryNodeDef(Scope)
        val tagDef = FactoryNodeDef(Scope)
        val tagDefinition = s"${ inheritTagDef.toPattern }-[:`${ Inherits.relationType }`*0..10]->${ tagDef.toPattern }"
        preQueries +=
          s"""
          match ${ tagDefinition }
          where (${ tagDef.name }.uuid in {contextsAnyUuids})
          with distinct *
          """

          val relationDef = new RelationDef(inheritTagDef, SchemaTags, nodeDef) { override val name = "contextsAnytags"}
          postMatches += s"""${if(classificationsAny.nonEmpty) "optional" else ""} match ${ relationDef.toPattern(false, false) }"""

          params += ("contextsAnyUuids" -> contextsAny)
      }

      if(classificationsAny.nonEmpty) {
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
          ${if(contextsAny.nonEmpty) "or (contextsAnytags is not null)" else ""}
        )"""

        if(contextsAny.nonEmpty) lastDistinct = true

        params += ("classificationsAnyUuids" -> classificationsAny)
      }

      if(contextsWithout.nonEmpty) {
        val inheritTagDef = FactoryNodeDef(Scope)
        val tagDef = FactoryNodeDef(Scope)
        val tagDefinition = s"${ inheritTagDef.toPattern }-[:`${ Inherits.relationType }`*0..10]->${ tagDef.toPattern }"
        preQueries +=
          s"""
          match ${ tagDefinition }
          where ${ tagDef.name }.uuid in {contextsWithoutUuids}
          with distinct *
          """

          val relationDef = RelationDef(inheritTagDef, SchemaTags, nodeDef)
          postMatches += s"""optional match ${ relationDef.toPattern(false, false) }"""
          lastWiths += s"""count(${relationDef.name}) as contextsWithoutCount"""
          lastConditions += s"""contextsWithoutCount = 0"""

          params += ("contextsWithoutUuids" -> contextsWithout)
      }

      if(classificationsWithout.nonEmpty) {
        val classificationDef = FactoryNodeDef(Classification)
        preQueries += s"""
        match ${classificationDef.toPattern}
        where ${classificationDef.name}.uuid in {classificationsWithoutUuids}
        with *
        """

        val connectsDef = HyperNodeDef(nodeDef, Connects, FactoryNodeDef(Post))
        val classifiesConnectsDef = AnonRelationDef(classificationDef, Classifies, connectsDef)
        val tagsDef = HyperNodeDef(FactoryNodeDef(Scope), SchemaTags, nodeDef)
        val classifiesTagsDef = AnonRelationDef(classificationDef, Classifies, tagsDef)
        postMatches += s"""
        optional match ${connectsDef.toPattern(false, true)}, ${ classifiesConnectsDef.toPattern(false) }
        optional match ${tagsDef.toPattern(true, false)}, ${ classifiesTagsDef.toPattern(false) }
        with *
        """

        lastWiths += s"""count(${connectsDef.name}) + count(${tagsDef.name}) as classificationsWithoutMatched"""
        lastConditions += s"""classificationsWithoutMatched = 0"""

        params += ("classificationsWithoutUuids" -> classificationsWithout)
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
      ${if(postConditions.nonEmpty) s"where ${postConditions.mkString("\nand ")}" else ""}
      ${if(lastWiths.nonEmpty) s"with ${nodeDef.name}, ${lastWiths.mkString(", ")}" else ""}
      ${if(lastConditions.nonEmpty) s"where ${lastConditions.mkString("\nand ")}" else ""}
      ${if(lastDistinct) s"with distinct ${nodeDef.name}" else ""}
      return $returnStatement
      """

      println("-"*30)
      // println(query)
      // println(ctx.params ++ params)
      def parameterToString(p:Any):String = p match {
        // case array:List[String] => s"[${array.map(parameterToString).mkString(",")}]"
        case ArrayParameterValue(array) => s"[${array.map(parameterToString).mkString(",")}]"
        case StringPropertyValue(str) => s""""${str}""""
        case v => v.toString
      }
      println((ctx.params ++ params).map{case(k,v) => (s"\\{$k\\}", parameterToString(v))}.foldLeft(query){case (z, (s,r)) => z.replaceAll(s, r)})
      println("-"*30)
      try{
        // When Neo4j throws an error because the regexp is incorrect, return an empty Discourse instead
        Discourse(db.queryGraph(query, ctx.params ++ params))
      } catch {
        case e:Exception =>
          println(e.getMessage) //TODO: use logger
          val discourse = Discourse.empty
          discourse.add(Post.create(s"""Search Error. Please report this to us."""))
          //TODO: automatically report: request.uri and error message
          discourse
      }
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
