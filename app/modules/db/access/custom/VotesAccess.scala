package modules.db.access.custom

import model.WustSchema._
import modules.db.Database._
import modules.db.access.EndRelationFactoryAccess
import modules.db.{HyperNodeDefinitionBase, RelationDefinition}
import play.api.libs.json.JsValue
import renesca.schema._

//TODO: we need to merge here with onMatch = Set("weight")
class VotesAccess(
  weight: Long
  ) extends EndRelationFactoryAccess[User, Votes, Categorizes] {
  val factory = Votes
  val nodeFactory = User

  override def createHyper(startUuid: String, endUuid: String, user: User, json: JsValue) = {
    Right("")
  }
}


object VotesAccess {
  def apply(weight: Long): UuidNodeFactory[Categorizes] => VotesAccess = {
    _ => new VotesAccess(weight)
  }
}
