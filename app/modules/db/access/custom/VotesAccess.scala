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

case class VotesUpdatedAccess(
  sign: Long
  ) extends EndRelationAccessDefault[User, VotesChangeRequest, ChangeRequest] {
  val nodeFactory = User

  override def create(context: RequestContext, param: ConnectParameter[ChangeRequest]) = context.withUser { user =>
    val success = if (sign == 0) {
      // first we define the updated relation between user and post that defines the actual change request
      val start = ConcreteFactoryNodeDefinition(User)
      val end = ConcreteFactoryNodeDefinition(Post)
      val changeRequest = HyperNodeDefinition(start, Updated, end, Some(param.baseUuid))

      // next we define the voting relation between the currently logged in user and the change request
      val userNode = ConcreteNodeDefinition(user)
      val votes = RelationDefinition(userNode, VotesChangeRequest, changeRequest)
      disconnectNodes(votes)
      true
    } else {
      val start = User.matches()
      val end = Post.matches()
      //TODO: should have matchesOnUuid(start, end, uuid)
      val changeRequest = Updated.matches(start, end, uuid = Some(param.baseUuid), matches = Set("uuid"))
      val votes = VotesChangeRequest.merge(user, changeRequest, weight = sign, onMatch = Set("weight"))
      val failure = db.transaction(_.persistChanges(start, end, changeRequest, votes))
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

//TODO: code duplication, we need hyperrelation traits in magic, in order to be
//able to match the ChangeRequest trait and have the correct type: Relation+Node
case class VotesUpdatedTagsAccess(
  sign: Long
  ) extends EndRelationAccessDefault[User, VotesChangeRequest, ChangeRequest] {
  val nodeFactory = User

  override def create(context: RequestContext, param: ConnectParameter[ChangeRequest]) = context.withUser { user =>
    val success = if (sign == 0) {
      // first we define the updated relation between user and post that defines the actual change request
      val start = ConcreteFactoryNodeDefinition(User)
      val end = ConcreteFactoryNodeDefinition(Post)
      val changeRequest = HyperNodeDefinition(start, UpdatedTags, end, Some(param.baseUuid))

      // next we define the voting relation between the currently logged in user and the change request
      val userNode = ConcreteNodeDefinition(user)
      val votes = RelationDefinition(userNode, VotesChangeRequest, changeRequest)
      disconnectNodes(votes)
      true
    } else {
      val start = User.matches()
      val end = Post.matches()
      //TODO: should have matchesOnUuid(start, end, uuid)
      val changeRequest = UpdatedTags.matches(start, end, uuid = Some(param.baseUuid), matches = Set("uuid"))
      val votes = VotesChangeRequest.merge(user, changeRequest, weight = sign, onMatch = Set("weight"))
      val failure = db.transaction(_.persistChanges(start, end, changeRequest, votes))
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
