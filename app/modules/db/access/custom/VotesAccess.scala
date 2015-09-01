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

case class VotesOnUpdatedAccess(
  sign: Long
  ) extends EndRelationAccessDefault[User, VotesOnUpdated, Updated] {
  val nodeFactory = User

  //TODO: nicer interface for custom access, here we know more...
  override def create(context: RequestContext, param: ConnectParameter[Updated]) = context.withUser { user =>
    val success = if (sign == 0) {
      // first we define the updated relation between user and post that defines the actual change request
      val start = ConcreteFactoryNodeDefinition(User)
      val end = ConcreteFactoryNodeDefinition(Post)
      val changeRequest = HyperNodeDefinition(start, Updated, end, Some(param.baseUuid))

      // next we define the voting relation between the currently logged in user and the change request
      val userNode = ConcreteNodeDefinition(user)
      val votes = RelationDefinition(userNode, VotesOnUpdated, changeRequest)
      disconnectNodes(votes)
      true
    } else {
      val start = User.matches()
      val end = Post.matches()
      //TODO: should have matchesOnUuid(start, end, uuid)
      val changeRequest = Updated.matches(start, end, uuid = Some(param.baseUuid), matches = Set("uuid"))
      val votes = VotesOnUpdated.merge(user, changeRequest, weight = sign, onMatch = Set("weight"))
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
