package modules.db.access.custom

import controllers.api.nodes.{HyperConnectParameter, RequestContext}
import model.WustSchema._
import modules.db.Database._
import modules.db.access.{EndRelationAccessDefault, EndRelationAccess}
import modules.requests.ConnectResponse
import play.api.libs.json._
import renesca.parameter.implicits._
import renesca.schema._
import play.api.mvc.Results._

case class VotesAccess(
  weight: Long
  ) extends EndRelationAccessDefault[User, Votes, Dimensionizes] {
  val nodeFactory = User

  //TODO: nicer interface for custom access, here we know more...
  override def create[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S,Dimensionizes with AbstractRelation[S,E],E]) = {
    //TODO: should check whether there is a tags relation between start end before voting
    val start = VoteDimension.matchesOnUuid(param.startUuid)
    val end = Votable.matchesOnUuid(param.endUuid)
    val hyper = Dimensionizes.merge(start, end)
    val votes = Votes.merge(context.user, hyper, weight = weight, onMatch = Set("weight"))
    val failure = db.transaction(_.persistChanges(start, end, hyper, votes))
    if(failure.isDefined)
      Left(BadRequest("No vote :/"))
    else
      Left(Ok(JsObject(Seq(
        ("weight", JsNumber(weight))
      ))))
  }
}
