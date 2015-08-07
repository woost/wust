package modules.db.access.custom

import controllers.api.nodes.RequestContext
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
  val baseFactory = Categorizes

  override def createHyper(context: RequestContext, startUuid: String, endUuid: String) = {
    val start = TagLikeMatches.matches(uuid = Some(startUuid), matches = Set("uuid"))
    val end = Taggable.matches(uuid = Some(endUuid), matches = Set("uuid"))
    val hyper = Categorizes.matches(start, end)
    val votes = Votes.merge(context.user, hyper, weight = weight, onMatch = Set("weight"))
    val failure = db.transaction(_.persistChanges(start, end, hyper, votes))
    if(failure.isDefined)
      Right("No vote :/")
    else
      Left(ConnectResponse(Discourse.empty, Some(context.user)))
  }
}


object VotesAccess {
  def apply(weight: Long) = new VotesAccess(weight)
}
