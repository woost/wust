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

  override def create(context: RequestContext, param: ConnectParameter[ChangeRequest]) = context.withUser { user =>
    val success = if (sign == 0) {
      // first we match the actual change request
      val changeRequest = nodeDefinition(param.baseUuid)

      // next we define the voting relation between the currently logged in user and the change request
      val userNode = ConcreteNodeDefinition(user)
      val votes = RelationDefinition(userNode, VotesChangeRequest, changeRequest)
      disconnectNodesFor(votes)
    } else {
      val changeRequest = matchesNode(param.baseUuid)
      val votes = VotesChangeRequest.merge(user, changeRequest, weight = sign, onMatch = Set("weight"))
      val failure = db.transaction(_.persistChanges(changeRequest, votes))
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
