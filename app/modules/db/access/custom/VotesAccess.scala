package modules.db.access.custom

import controllers.api.nodes.{ConnectParameter, RequestContext}
import model.WustSchema._
import modules.db.Database._
import modules.db._
import modules.db.access.{EndRelationAccessDefault, EndRelationAccess}
import modules.requests.ConnectResponse
import formatters.json.ApiNodeFormat._
import play.api.libs.json._
import renesca.parameter.implicits._
import renesca.schema._
import renesca._
import play.api.mvc.Results._

trait VotesAccessBase[T <: ChangeRequest] extends EndRelationAccessDefault[User, VotesChangeRequest, ChangeRequest] {
  val sign: Long
  val nodeFactory = User

  def nodeDefinition(uuid: String): FactoryUuidNodeDefinition[T]
  def selectNode(discourse: Discourse): T
  def applyChange(request: T, post: Post): Boolean

  //TODO: optimize to one request with multiple statements
  override def create(context: RequestContext, param: ConnectParameter[ChangeRequest]) = context.withUser { user =>
    db.transaction { tx =>
      // first we match the actual change request and aquire a write lock,
      // which will last for the whole transaction
      val requestDef = nodeDefinition(param.baseUuid)
      val userDef = ConcreteNodeDefinition(user)
      val votesDef = RelationDefinition(userDef, VotesChangeRequest, requestDef)
      val discourse = Discourse(tx.queryGraph(Query(s"match ${requestDef.toQuery}-[:`${UpdatedToPost.relationType}`|`${UpdatedTagsToPost.relationType}`]->(post:`${Post.label}`) set ${requestDef.name}.__lock = true with post,${requestDef.name} optional match ${votesDef.toQuery(true, false)} return post,${requestDef.name}, ${votesDef.name}", votesDef.parameterMap)))
      val request = selectNode(discourse)
      val votes = discourse.votesChangeRequests.headOption
      votes.foreach(request.applyVotes -= _.weight)

      val weight = sign // TODO karma
      if (weight == 0) {
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

      val postApplies = if (request.applyVotes >= request.applyThreshold) {
        val post = discourse.posts.head
        request.applied = applyChange(request, post)
        if (request.applied)
          Some(Json.toJson(post))
        else
          None
      } else {
        Some(JsNull)
      }

      Left(postApplies.map { node =>
        request.rawItem.properties -= "__lock"
        val failure = tx.persistChanges(discourse)

        failure.map(_ => BadRequest("No vote :/")).getOrElse {
          Ok(JsObject(Seq(
            ("weight", JsNumber(weight)),
            ("node", node)
          )))
        }
      }.getOrElse {
        tx.rollback()
        BadRequest("Cannot apply changes automatically")
      })
    }
  }
}

//TODO: we need hyperrelation traits in magic in order to matches on the hyperrelation trait and get correct type: Relation+Node
case class VotesUpdatedAccess(
  sign: Long
  ) extends VotesAccessBase[Updated] {
    override def nodeDefinition(uuid: String) = FactoryUuidNodeDefinition(Updated, uuid)
    override def selectNode(discourse: Discourse) = discourse.updateds.head
    override def applyChange(request: Updated, post: Post) = {
      if (post.title != request.oldTitle || post.description != request.oldDescription) {
        false
      } else {
        post.title = request.newTitle
        post.description = request.newDescription
        request.applied = true
        true
      }
    }
}

case class VotesUpdatedTagsAccess(
  sign: Long
  ) extends VotesAccessBase[UpdatedTags] {
    override def nodeDefinition(uuid: String) = FactoryUuidNodeDefinition(UpdatedTags, uuid)
    override def selectNode(discourse: Discourse) = discourse.updatedTags.head
    override def applyChange(request: UpdatedTags, post: Post) = {
        throw new Exception("Applying change requests for tags currently not possible")
    }
}
