package modules.db.access

import model.WustSchema._
import modules.db.Database._
import modules.db.RelationDefinition
import modules.db.types.UuidHyperNodeDefinitionBase
import play.api.libs.json.JsValue
import renesca.schema._

//TODO: we need to merge here with onMatch = Set("weight")
class VotesAccess(
  override val factory: Votes.type,
  val weight: Long
  ) extends EndRelationFactoryAccess[User, Votes, Categorizes] {
  val nodeFactory = User

  override def createHyper(baseDef: UuidHyperNodeDefinitionBase[Categorizes with AbstractRelation[_, _]], user: User, json: JsValue) = {
    val relationDefinition = RelationDefinition(toNodeDefinition(user.uuid), factory, baseDef)
    val resultOpt = endConnectHyperNodesToVotes(relationDefinition, weight)
    resultOpt match {
      case Some((start, _)) => {
        //        Broadcaster.broadcastEndHyperConnect(relationDefinition, start)
        Left(start)
      }
      case None             => Right("Cannot vote")
    }
  }
}
