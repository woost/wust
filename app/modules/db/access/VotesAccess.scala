package modules.db.access

import model.WustSchema._
import modules.db.Database._
import modules.db.{HyperNodeDefinitionBase, RelationDefinition}
import play.api.libs.json.JsValue
import renesca.schema._

//TODO: we need to merge here with onMatch = Set("weight")
class VotesAccess(
  weight: Long
  ) extends EndRelationFactoryAccess[User, Votes, Categorizes] {
  val factory = Votes
  val nodeFactory = User

  override def createHyper(baseDef: HyperNodeDefinitionBase[Categorizes with AbstractRelation[_, _]], user: User, json: JsValue) = {
    val relationDefinition = RelationDefinition(toNodeDefinition(user.uuid), factory, baseDef)
    val resultOpt = endConnectHyperNodesToVotes(relationDefinition, weight)
    resultOpt match {
      case Some((start, _)) => Left(start)
      case None             => Right("Cannot vote")
    }
  }
}


object VotesAccess {
  def apply(weight: Long): UuidNodeFactory[Categorizes] => VotesAccess = {
    _ => new VotesAccess(weight)
  }
}
