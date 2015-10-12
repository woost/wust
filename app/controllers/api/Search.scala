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
import moderation.Moderation
import wust.SortOrder

import scala.util.Try

object Search extends Controller {
  def index(labelOpt: Option[String], termOpt: Option[String], searchDescriptionsOpt: Option[Boolean], startPostOpt: Option[Boolean], contextsAll: List[String], contextsAnyRaw: List[String], contextsWithout: List[String], classificationsAll: List[String], classificationsAnyRaw: List[String], classificationsWithout: List[String], pageOpt: Option[Int], sizeOpt: Option[Int], sortOrder: Option[Int]) = Action {

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
      val sortByQuality = sortOrder.map(_ == SortOrder.QUALITY).getOrElse(false)

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

      def matchInheritedContexts(contexts:List[String], uuidParamName:String) = {
        val askedContextDef = FactoryNodeDef(Scope)
        val inheritedContextDef = FactoryNodeDef(Scope)
        val inheritancePath = s"${ inheritedContextDef.toPattern }-[:`${ Inherits.relationType }`*0..10]->${ askedContextDef.toPattern }"
        preQueries +=
          s"""
          match ${ inheritancePath }
          where ${ askedContextDef.name }.uuid in {$uuidParamName}
          with *
          """
        params += (uuidParamName-> contexts)

        (askedContextDef, inheritedContextDef)
      }

      def matchClassifications(classifications:List[String], uuidParamName:String, matchClassifies:Boolean) = {
        val classificationDef = FactoryNodeDef(Classification)
        preQueries += s"""
          match ${classificationDef.toPattern}
          where ${classificationDef.name}.uuid in {$uuidParamName}
          with *
          """
        params += (uuidParamName-> classifications)

        val connectsDef = HyperNodeDef(nodeDef, Connects, FactoryNodeDef(Post))
        val classifiesConnectsDef = AnonRelationDef(classificationDef, Classifies, connectsDef)
        val tagsDef = HyperNodeDef(FactoryNodeDef(Scope), SchemaTags, nodeDef)
        val classifiesTagsDef = AnonRelationDef(classificationDef, Classifies, tagsDef)

        if(matchClassifies) {
          postMatches += s"""
          optional match ${connectsDef.toPattern(false, true)}, ${ classifiesConnectsDef.toPattern(false) }
          optional match ${tagsDef.toPattern(true, false)}, ${ classifiesTagsDef.toPattern(false) }
          with *
          """
        } else {
          postMatches += s"""
          optional match ${connectsDef.toPattern(false, true)}
          optional match ${tagsDef.toPattern(true, false)}
          with *
          """
        }
        (connectsDef, tagsDef, classifiesConnectsDef, classifiesTagsDef)
      }

      if(labelOpt.contains(Post.label.name)) {
        if(contextsAll.nonEmpty) {
          val (askedContextDef, inheritedContextDef) = matchInheritedContexts(contextsAll, "contextsAllUuids")
          val tagsDef = RelationDef(inheritedContextDef, SchemaTags, nodeDef)
          postMatches += s"""match ${ tagsDef.toPattern(false) }"""
          lastWiths += s"""count(distinct ${askedContextDef.name}) as contextsAllMatchCount, collect(${tagsDef.name}) as contextsAllTagsColl"""
          lastConditions += s"""contextsAllMatchCount >= {contextsAllCount}"""
          params += ("contextsAllCount" -> contextsAll.size)
        }

        if(classificationsAll.nonEmpty) {
          val (connectsDef, tagsDef, _, _) = matchClassifications(classificationsAll, "classificationsAllUuids", matchClassifies = true)
          lastWiths += s"""count(${connectsDef.name}) + count(${tagsDef.name}) as classificationsAllMatchCount"""
          lastConditions += s"""classificationsAllMatchCount >= {classificationsAllCount}"""
          params += ("classificationsAllCount" -> classificationsAll.size)
        }

        if(contextsAny.nonEmpty) {
          val (_, inheritedContextDef) = matchInheritedContexts(contextsAny, "contextsAnyUuids")
          val tagsDef = new RelationDef(inheritedContextDef, SchemaTags, nodeDef) { override val name = "contextsAnytags"}
          postMatches += s"""${if(classificationsAny.nonEmpty) "optional" else ""} match ${ tagsDef.toPattern(false, false) }"""
        }

        if(classificationsAny.nonEmpty) {
          val (connectsDef, tagsDef, classifiesConnectsDef, classifiesTagsDef) = matchClassifications(classificationsAny, "classificationsAnyUuids", matchClassifies = false)
          postConditions += s"""(
            (${connectsDef.endName} is not null and ${ classifiesConnectsDef.toPattern(false) })
            or (${tagsDef.startName} is not null and ${ classifiesTagsDef.toPattern(false) })
            ${if(contextsAny.nonEmpty) "or (contextsAnytags is not null)" else ""}
          )"""
          if(contextsAny.nonEmpty) lastDistinct = true
        }

        if(contextsWithout.nonEmpty) {
          val (_, inheritedContextDef) = matchInheritedContexts(contextsWithout, "contextsWithoutUuids")
          val tagsDef = RelationDef(inheritedContextDef, SchemaTags, nodeDef)
          postMatches += s"""optional match ${ tagsDef.toPattern(false, false) }"""
          lastWiths += s"""count(${tagsDef.name}) as contextsWithoutCount"""
          lastConditions += s"""contextsWithoutCount = 0"""
        }

        if(classificationsWithout.nonEmpty) {
          val (connectsDef, tagsDef, _, _) = matchClassifications(classificationsWithout, "classificationsWithoutUuids", matchClassifies = true)
          lastWiths += s"""count(${connectsDef.name}) + count(${tagsDef.name}) as classificationsWithoutMatchCount"""
          lastConditions += s"""classificationsWithoutMatchCount = 0"""
        }
      }

      val returnPostfix = sizeOpt.map { limit =>
        val skip = page * limit
        s"skip $skip limit $limit"
      }.getOrElse("")
      val returnStatement = if (contextsAll.nonEmpty && sortByQuality)
        s"""
        with unwind contextsAllTagsColl as contextsAllTags, min(contextsAllTags.voteCount) as tagVoteCount, ${ nodeDef.name } order by ${Moderation.postQualityString("tagVoteCount", nodeDef.name + ".viewCount")} desc $returnPostfix
        return ${nodeDef.name}
        """
      else
        s"return ${ nodeDef.name } order by ${ nodeDef.name }.timestamp desc $returnPostfix"


      val query = s"""
      ${preQueries.mkString("\n\n")}
      match ${ nodeDef.toPattern }
      ${postMatches.mkString("\n")}
      ${if(postConditions.nonEmpty) s"where ${postConditions.mkString("\nand ")}" else ""}
      ${if(lastWiths.nonEmpty) s"with ${nodeDef.name}, ${lastWiths.mkString(", ")}" else ""}
      ${if(lastConditions.nonEmpty) s"where ${lastConditions.mkString("\nand ")}" else ""}
      ${if(lastDistinct) s"with distinct ${nodeDef.name}" else ""}
      $returnStatement
      """


      // println("-"*30)
      // // println(query)
      // // println(ctx.params ++ params)
      // def parameterToString(p:Any):String = p match {
      //   // case array:List[String] => s"[${array.map(parameterToString).mkString(",")}]"
      //   case ArrayParameterValue(array) => s"[${array.map(parameterToString).mkString(",")}]"
      //   case StringPropertyValue(str) => s""""${str}""""
      //   case v => v.toString
      // }
      // println((ctx.params ++ params).map{case(k,v) => (s"\\{$k\\}", parameterToString(v))}.foldLeft(query){case (z, (s,r)) => z.replaceAll(s, r)})
      // println("-"*30)
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
