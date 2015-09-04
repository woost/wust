package modules.db.access.custom

import controllers.api.nodes.{ConnectParameter, RequestContext}
import model.WustSchema._
import modules.db.Database._
import modules.db._
import modules.db.access.{EndRelationAccessDefault, EndRelationAccess}
import modules.requests.ConnectResponse
import play.api.libs.json._
import renesca.parameter.implicits._
import renesca.schema._
import play.api.mvc.Results._

trait VotesAccessBase extends EndRelationAccessDefault[User, VotesChangeRequest, ChangeRequest] {
  val sign: Long
  val nodeFactory = User

  def nodeDefinition(uuid: String): FactoryUuidNodeDefinition[ChangeRequest]
  def matchesNode(uuid: String): ChangeRequest

  //TODO: optimize to one request with multiple statements
  override def create(context: RequestContext, param: ConnectParameter[ChangeRequest]) = context.withUser { user =>
    db.transaction { tx =>
      // first we match the actual change request and aquire a write lock,
      // which will last for the whole transaction
      val changeRequest = matchesNode(param.baseUuid)
      changeRequest.rawItem.properties += "__lock" -> true
      tx.persistChanges(changeRequest)
      val success = if (sign == 0) {
        val requestDef = nodeDefinition(param.baseUuid)
        // next we define the voting relation between the currently logged in
        // the relation should be deleted, as we want to unvote.
        val userDef = ConcreteNodeDefinition(user)
        val votes = RelationDefinition(userDef, VotesChangeRequest, requestDef)
        disconnectNodesFor(votes, tx)
      } else {
        // we want to vote on the change request with our weight. we merge the
        // votes relation as we want to override any previous vote
        val votes = VotesChangeRequest.merge(user, changeRequest, weight = sign, onMatch = Set("weight"))
        changeRequest.rawItem.properties -= "__lock"
        val failure = tx.persistChanges(changeRequest, votes)
        !failure.isDefined
      }

      Left(if (success)
        Ok(JsObject(Seq(
          ("weight", JsNumber(sign))
        )))
      else
        BadRequest("No vote :/")
      )
    }
  }
}

//TODO: we need hyperrelation traits in magic in order to matches on the hyperrelation trait and get correct type: Relation+Node
case class VotesUpdatedAccess(
  sign: Long
  ) extends VotesAccessBase {
    override def nodeDefinition(uuid: String) = FactoryUuidNodeDefinition(Updated, uuid)
    //TODO: need matchesOnUuid! for hyperrelations
    override def matchesNode(uuid: String) = Updated.matchesUuidNode(uuid = Some(uuid), matches = Set("uuid"))
}

case class VotesUpdatedTagsAccess(
  sign: Long
  ) extends VotesAccessBase {
    override def nodeDefinition(uuid: String) = FactoryUuidNodeDefinition(UpdatedTags, uuid)
    override def matchesNode(uuid: String) = UpdatedTags.matchesUuidNode(uuid = Some(uuid), matches = Set("uuid"))
}
