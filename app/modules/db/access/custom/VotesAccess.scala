package modules.db.access.custom

import controllers.api.nodes.{HyperConnectParameter, RequestContext}
import model.WustSchema._
import modules.db.Database._
import modules.db.access.{EndRelationAccessDefault, EndRelationAccess}
import modules.requests.ConnectResponse
import play.api.libs.json.JsValue
import renesca.parameter.implicits._
import renesca.schema._

case class VotesAccess(
  weight: Long
  ) extends EndRelationAccessDefault[User, Votes, Categorizes] {
  val nodeFactory = User

  //TODO: nicer interface for custom access, here we know more...
  override def create[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S,Categorizes with AbstractRelation[S,E],E]) = {
    val start = param.startFactory.matchesOnUuid(param.startUuid)
    val end = param.endFactory.matchesOnUuid(param.endUuid)
    val hyper = param.baseFactory.matchesHyperConnection(start, end)
    val votes = Votes.merge(context.user, hyper, weight = weight, onMatch = Set("weight"))
    val failure = db.transaction(_.persistChanges(start, end, hyper, votes))
    if(failure.isDefined)
      Left("No vote :/")
    else
      Right(ConnectResponse(Discourse.empty, Some(context.user)))
  }
}
