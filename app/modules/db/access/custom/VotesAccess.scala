package modules.db.access.custom

import model.WustSchema._
import modules.db.Database._
import modules.db.access.EndRelationAccess
import modules.requests.ConnectResponse
import play.api.libs.json.JsValue
import renesca.parameter.implicits._

class VotesAccess(
  weight: Long
  ) extends EndRelationAccess[User, Votes, Categorizes] {
  val factory = Votes
  val nodeFactory = User

  override def createHyper(startUuid: String, endUuid: String, user: User, json: JsValue) = {
    val start = Tag.matches(uuid = Some(startUuid), matches = Set("uuid"))
    val end = Taggable.matches(uuid = Some(endUuid), matches = Set("uuid"))
    val hyper = Categorizes.matches(start, end)
    val votes = Votes.merge(user, hyper, weight = weight, onMatch = Set("weight"))
    val failure = db.transaction(_.persistChanges(start, end, hyper, votes))
    if(failure.isDefined)
      Right("No vote :/")
    else
      Left(ConnectResponse(Discourse.empty, Some(user)))
  }
}


object VotesAccess {
  def apply(weight: Long): UuidNodeFactory[Categorizes] => VotesAccess = {
    _ => new VotesAccess(weight)
  }
}
