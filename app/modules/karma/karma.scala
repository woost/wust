package modules.karma

import modules.db.Database.db
import model.Helpers
import model.WustSchema._
import modules.db.{QueryContext, NodeDefinition}
import renesca.QueryHandler
import renesca.parameter.ParameterMap
import renesca.parameter.implicits._

case class KarmaQuery(postDef: NodeDefinition[Post], userDef: NodeDefinition[User], matcher: String, params: ParameterMap)

case class KarmaTagMatcher(tagDef: NodeDefinition[Scope], matcher: String)

case class KarmaDefinition(karmaChange: Long, reason: String)

object KarmaUpdate {
  import play.api.libs.concurrent.Execution.Implicits._
  import scala.concurrent.Future

  //TODO: log failure
  def persist(karmaDefinition: KarmaDefinition, karmaQuery: KarmaQuery, karmaTagMatcher: KarmaTagMatcher)(implicit ctx: QueryContext) = {
    db.transaction { tx =>
      // TODO: code dup from discourse.scala
      val timestamp = System.currentTimeMillis;
      val uuid = Helpers.uuidBase64
      val logLabels = KarmaLog.labels.map(l => s":`$l`").mkString

      val query = s"""
      ${ karmaQuery.matcher }
      create (${karmaQuery.userDef.name})-[:`${KarmaLog.startRelationType}`]->(karmaLog $logLabels {karmaProps})-[:`${KarmaLog.endRelationType}`]->(${karmaQuery.postDef.name})
      with *
      ${ karmaTagMatcher.matcher }
      merge (${karmaQuery.userDef.name})-[r:`${HasKarma.relationType}`]->(${karmaTagMatcher.tagDef.name})
      on create set r.karma = {karmaProps}.karmaChange
      on match set r.karma = r.karma + {karmaProps}.karmaChange
      create (karmaLog)-[logonscope:`${LogOnScope.relationType}`]->(${karmaTagMatcher.tagDef.name}) set logonscope.currentKarma = r.karma
      """

      val params = karmaQuery.params ++ Map(
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
}
