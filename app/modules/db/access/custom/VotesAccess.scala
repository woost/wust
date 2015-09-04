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
import renesca._
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
      val requestDef = nodeDefinition(param.baseUuid)
      val userDef = ConcreteNodeDefinition(user)
      val votesDef = RelationDefinition(userDef, VotesChangeRequest, requestDef)
      val discourse = Discourse(tx.queryGraph(Query(s"match ${requestDef.toQuery} set ${requestDef.name}.__lock = true with ${requestDef.name} optional match ${votesDef.toQuery(true, false)} return ${requestDef.name}, ${votesDef.name}", votesDef.parameterMap)))
      val request = discourse.changeRequests.head
      val votes = discourse.votesChangeRequests.headOption
      votes.foreach(request.applyVotes -= _.weight)

      val weight = sign // TODO karma
      if (request.applyVotes + weight >= request.applyThreshold)
        throw new Exception("Applying change requests currently not possible")

      if (sign == 0) {
        // if there are any existing votes, disconnect them
        votes.foreach(discourse.graph.relations -= _.rawItem)
      } else {
        // we want to vote on the change request with our weight. we merge the
        // votes relation as we want to override any previous vote. merging is
        // better than just updating the weight on an existing relation, as it
        // guarantees uniqueness
        request.applyVotes += weight
        val newVotes = VotesChangeRequest.merge(user, request, weight = weight, onMatch = Set("weight"))
        discourse.add(newVotes)
      }

      request.rawItem.properties -= "__lock"
      val failure = tx.persistChanges(discourse)

      Left(if (failure.isEmpty)
        Ok(JsObject(Seq(
          ("weight", JsNumber(weight))
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
