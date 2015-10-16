package modules.karma

import controllers.live.LiveWebSocket
import modules.db.Database.db
import model.Helpers
import model.WustSchema._
import modules.db._
import modules.db.types._
import renesca.QueryHandler
import renesca.parameter.implicits._
import common.Constants

trait KarmaQuery {
  def matcher: String
  def userDef: NodeDef[User]
  def postDef: NodeDef[Post]
}

case class KarmaQueryCreated(createdDef: NodeRelationDef[User, Created, Post]) extends KarmaQuery {
  def matcher = createdDef.toPattern
  def userDef = createdDef.startDefinition
  def postDef = createdDef.endDefinition
}
case class KarmaQueryUserPost(userDef: NodeDef[User], postDef: NodeDef[Post]) extends KarmaQuery {
  def matcher = s"${userDef.toPattern}, ${postDef.toPattern}"
}

case class KarmaDefinition(karmaChange: Long, reason: String)

object KarmaUpdate {
  import play.api.libs.concurrent.Execution.Implicits._
  import scala.concurrent.Future

  private case class KarmaTagQuery(tagDef: NodeDef[Scope], matcher: String)

  //TODO: log failure
  private def persist(karmaDefinition: KarmaDefinition, karmaQuery: KarmaQuery, karmaTagQuery: KarmaTagQuery)(implicit ctx: QueryContext) = Future {
    db.transaction { tx =>
      // TODO: code dup from discourse.scala
      val timestamp = System.currentTimeMillis;
      val uuid = Helpers.uuidBase64
      val logLabels = KarmaLog.labels.map(l => s":`$l`").mkString

      val query = s"""
      match ${ karmaQuery.matcher }
      create (${karmaQuery.userDef.name})-[karmaLog1:`${KarmaLog.startRelationType}`]->(karmaLog $logLabels {karmaProps})-[karmaLog2:`${KarmaLog.endRelationType}`]->(${karmaQuery.postDef.name})
      with ${karmaQuery.userDef.name}, ${karmaQuery.postDef.name}, karmaLog, karmaLog1, karmaLog2
      ${ karmaTagQuery.matcher }
      merge (${karmaQuery.userDef.name})-[r:`${HasKarma.relationType}`]->(${karmaTagQuery.tagDef.name})
      on create set r.karma = {karmaProps}.karmaChange
      on match set r.karma = r.karma + {karmaProps}.karmaChange
      create (karmaLog)-[logonscope:`${LogOnScope.relationType}`]->(${karmaTagQuery.tagDef.name}) set logonscope.currentKarma = r.karma
      return *
      """

      val params = ctx.params ++ Map(
        "karmaProps" -> Map(
          "uuid" -> uuid,
          "timestamp" -> timestamp,
          "karmaChange" -> karmaDefinition.karmaChange,
          "reason" -> karmaDefinition.reason
        )
      )

      println(params)
      println(query)

      try {
        val discourse = Discourse(tx.queryGraph(query, params))

        println(discourse)
        LiveWebSocket.sendKarmaUpdate(discourse.karmaLogs)

      } catch {
        case e: Exception => println("EXCEPTION WHILE UPDATING KARMA:\n" + e)
      }
    }
  }

  def persistWithTags(karmaDefinition: KarmaDefinition, karmaQuery: KarmaQuery, tagDef: NodeDef[Scope])(implicit ctx: QueryContext) = {
    val tagMatch = s"match ${tagDef.toPattern}"
    val karmaTagQuery = KarmaTagQuery(tagDef, tagMatch)
    persist(karmaDefinition, karmaQuery, karmaTagQuery)
  }

  def persistWithProposedTags(karmaDefinition: KarmaDefinition, karmaQuery: KarmaQuery, tagsDef: NodeRelationDef[TagChangeRequest, ProposesTag, Scope])(implicit ctx: QueryContext) = {
    val tagMatch = s"match ${tagsDef.toPattern}"
    val karmaTagQuery = KarmaTagQuery(tagsDef.endDefinition, tagMatch)
    persist(karmaDefinition, karmaQuery, karmaTagQuery)
  }

  def persistWithConnectedTags(karmaDefinition: KarmaDefinition, karmaQuery: KarmaQuery)(implicit ctx: QueryContext) = {
    val tagDef = FactoryNodeDef(Scope)

    val tagMatch = s"""
    match (${karmaQuery.postDef.name})-[:`${Connects.startRelationType}`|`${Connects.endRelationType}` *0..${Constants.karmaTagDepth * 2}]->(connectable: `${Connectable.label}`)
    with distinct connectable, ${karmaQuery.userDef.name}, ${karmaQuery.postDef.name}, karmaLog, karmaLog1, karmaLog2
    match ${tagDef.toPattern}-[:`${Tags.startRelationType}`]->(:`${Tags.label}`)-[:`${Tags.endRelationType}`]->(connectable: `${Post.label}`)
    with distinct ${tagDef.name}, ${karmaQuery.userDef.name}, ${karmaQuery.postDef.name}, karmaLog, karmaLog1, karmaLog2
    """

    val karmaTagQuery = KarmaTagQuery(tagDef, tagMatch)
    persist(karmaDefinition, karmaQuery, karmaTagQuery)
  }

  def persistWithConnectedTagsThroughParent(karmaDefinition: KarmaDefinition, karmaQuery: KarmaQuery, parentDef: NodeDef[Connects])(implicit ctx: QueryContext) = {
    val tagDef = FactoryNodeDef(Scope)

    //TODO: do not match all scopes!
    val tagMatch = s"""
    match ${parentDef.toPattern}-[:`${Connects.startRelationType}`|`${Connects.endRelationType}` *0..${Constants.karmaTagDepth * 2 - 1}]->(connectable: `${Connectable.label}`)
    with distinct connectable, ${karmaQuery.userDef.name}, ${karmaQuery.postDef.name}, karmaLog, karmaLog1, karmaLog2
    match ${tagDef.toPattern}
    where (${tagDef.name})-[:`${Tags.startRelationType}`]->(:`${Tags.label}`)-[:`${Tags.endRelationType}`]->(${karmaQuery.postDef.name}) or (${tagDef.name})-[:`${Tags.startRelationType}`]->(:`${Tags.label}`)-[:`${Tags.endRelationType}`]->(connectable: `${Post.label}`)
    with distinct ${tagDef.name}, ${karmaQuery.userDef.name}, ${karmaQuery.postDef.name}, karmaLog, karmaLog1, karmaLog2
    """

    val karmaTagQuery = KarmaTagQuery(tagDef, tagMatch)
    persist(karmaDefinition, karmaQuery, karmaTagQuery)
  }

  def persistWithConnectedTagsOfHidden(karmaDefinition: KarmaDefinition, karmaQuery: KarmaQuery)(implicit ctx: QueryContext) = {
    val tagDef = FactoryNodeDef(Scope)

    val tagMatch = s"""
    optional match (${karmaQuery.postDef.name})-[:`${Connects.startRelationType}`|`${Connects.endRelationType}` *0..${Constants.karmaTagDepth * 2}]->(connectable: `${Connectable.label}`)
    with distinct connectable, ${karmaQuery.userDef.name}, ${karmaQuery.postDef.name}, karmaLog, karmaLog1, karmaLog2
    match ${tagDef.toPattern}
    where (${tagDef.name})-[:`${Tags.startRelationType}`]->(:`${Tags.label}`)-[:`${Tags.endRelationType}`]->(connectable: `${Post.label}`) or (${tagDef.name})-[:`${Tags.startRelationType}`]->(:`${Tags.label}`)-[:`${Tags.endRelationType}`]->(${karmaQuery.postDef.name})
    with distinct ${tagDef.name}, ${karmaQuery.userDef.name}, ${karmaQuery.postDef.name}, karmaLog, karmaLog1, karmaLog2
    """

    val karmaTagQuery = KarmaTagQuery(tagDef, tagMatch)
    persist(karmaDefinition, karmaQuery, karmaTagQuery)
  }
}
