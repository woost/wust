package modules.karma

import modules.db.Database.db
import model.Helpers
import model.WustSchema._
import modules.db._
import modules.db.types._
import renesca.QueryHandler
import renesca.parameter.ParameterMap
import renesca.parameter.implicits._

case class KarmaQuery(postDef: NodeDefinition[Post], userDef: NodeDefinition[User], matcher: String, params: ParameterMap)

case class KarmaTagQuery(tagDef: NodeDefinition[Scope], matcher: String, params: ParameterMap)

case class KarmaDefinition(karmaChange: Long, reason: String)

object KarmaUpdate {
  import play.api.libs.concurrent.Execution.Implicits._
  import scala.concurrent.Future

  //TODO: log failure
  private def persist(karmaDefinition: KarmaDefinition, karmaQuery: KarmaQuery, karmaTagQuery: KarmaTagQuery)(implicit ctx: QueryContext) = /*Future*/ {
    db.transaction { tx =>
      // TODO: code dup from discourse.scala
      val timestamp = System.currentTimeMillis;
      val uuid = Helpers.uuidBase64
      val logLabels = KarmaLog.labels.map(l => s":`$l`").mkString

      val query = s"""
      ${ karmaQuery.matcher }
      create (${karmaQuery.userDef.name})-[:`${KarmaLog.startRelationType}`]->(karmaLog $logLabels {karmaProps})-[:`${KarmaLog.endRelationType}`]->(${karmaQuery.postDef.name})
      with ${karmaQuery.userDef.name}, ${karmaQuery.postDef.name}, karmaLog
      ${ karmaTagQuery.matcher }
      merge (${karmaQuery.userDef.name})-[r:`${HasKarma.relationType}`]->(${karmaTagQuery.tagDef.name})
      on create set r.karma = {karmaProps}.karmaChange
      on match set r.karma = r.karma + {karmaProps}.karmaChange
      create (karmaLog)-[logonscope:`${LogOnScope.relationType}`]->(${karmaTagQuery.tagDef.name}) set logonscope.currentKarma = r.karma
      """

      val params = karmaQuery.params ++ karmaTagQuery.params ++ Map(
        "karmaProps" -> Map(
          "uuid" -> uuid,
          "timestamp" -> timestamp,
          "karmaChange" -> karmaDefinition.karmaChange,
          "reason" -> karmaDefinition.reason
        )
      )

      tx.query(query, params)
    }
  }

  def persistWithTags(karmaDefinition: KarmaDefinition, karmaQuery: KarmaQuery, tagDef: NodeDefinition[Scope])(implicit ctx: QueryContext) = {
    val tagMatch = s"match ${tagDef.toQuery}"
    val karmaTagQuery = KarmaTagQuery(tagDef, tagMatch, tagDef.parameterMap)
    persist(karmaDefinition, karmaQuery, karmaTagQuery)
  }

  def persistWithProposedTags(karmaDefinition: KarmaDefinition, karmaQuery: KarmaQuery, tagsDef: NodeRelationDefinition[TagChangeRequest, ProposesTag, Scope])(implicit ctx: QueryContext) = {
    val tagMatch = s"match ${tagsDef.toQuery}"
    val karmaTagQuery = KarmaTagQuery(tagsDef.endDefinition, tagMatch, tagsDef.parameterMap)
    persist(karmaDefinition, karmaQuery, karmaTagQuery)
  }

  def persistWithConnectedTags(karmaDefinition: KarmaDefinition, karmaQuery: KarmaQuery)(implicit ctx: QueryContext) = {
    val tagDef = ConcreteFactoryNodeDefinition(Scope)

    val tagMatch = s"""
    match (${karmaQuery.postDef.name})-[:`${Connects.startRelationType}`|`${Connects.endRelationType}` *0..20]->(connectable: `${Connectable.label}`)
    with distinct connectable, ${karmaQuery.userDef.name}, ${karmaQuery.postDef.name}, karmaLog
    match ${tagDef.toQuery}-[:`${Tags.startRelationType}`]->(:`${Tags.label}`)-[:`${Tags.endRelationType}`]->(connectable: `${Post.label}`)
    """

    val karmaTagQuery = KarmaTagQuery(tagDef, tagMatch, tagDef.parameterMap)
    persist(karmaDefinition, karmaQuery, karmaTagQuery)
  }
}
