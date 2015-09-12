package modules.db.access.custom

import controllers.api.nodes.{HyperConnectParameter, ConnectParameter, RequestContext}
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

trait VotesAccessBase[T <: ChangeRequest] extends EndRelationAccessDefault[User, Votes, Votable] {
  val sign: Long
  val nodeFactory = User

  def nodeDefinition(uuid: String): FactoryUuidNodeDefinition[T]
  def selectNode(discourse: Discourse): T
  def applyChange(discourse: Discourse, request: T, post: Post, tx:QueryHandler): Boolean

  //TODO: optimize to one request with multiple statements
  override def create(context: RequestContext, param: ConnectParameter[Votable]) = context.withUser { user =>
    db.transaction { tx =>
      // first we match the actual change request and aquire a write lock,
      // which will last for the whole transaction
      val requestDef = nodeDefinition(param.baseUuid)
      val userDef = ConcreteNodeDefinition(user)
      val votesDef = RelationDefinition(userDef, Votes, requestDef)
      val discourse = Discourse(tx.queryGraph(Query(s"match ${requestDef.toQuery}-[:`${UpdatedToPost.relationType}`|`${AddTagsToPost.relationType}`|`${RemoveTagsToPost.relationType}`]->(post:`${Post.label}`) set ${requestDef.name}.__lock = true with post,${requestDef.name} optional match ${votesDef.toQuery(true, false)} return post,${requestDef.name}, ${votesDef.name}", votesDef.parameterMap)))
      val request = selectNode(discourse)
      val votes = discourse.votes.headOption
      votes.foreach(request.approvalSum -= _.weight)

      val weight = sign // TODO karma
      if (weight == 0) {
        // if there are any existing votes, disconnect them
        votes.foreach(discourse.graph.relations -= _.rawItem)
      } else {
        // we want to vote on the change request with our weight. we merge the
        // votes relation as we want to override any previous vote. merging is
        // better than just updating the weight on an existing relation, as it
        // guarantees uniqueness
        request.approvalSum += weight
        val newVotes = Votes.merge(user, request, weight = weight, onMatch = Set("weight"))
        discourse.add(newVotes)
      }

      val postApplies = if (request.approvalSum >= request.applyThreshold) {
        val post = discourse.posts.head
        request.applied = applyChange(discourse, request, post, tx)
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
            ("vote", JsObject(Seq(
              ("weight", JsNumber(weight))
            ))),
            ("votes", JsNumber(request.approvalSum)),
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
    override def applyChange(discourse: Discourse, request: Updated, post: Post, tx:QueryHandler) = {
      val changesTitle = request.oldTitle != request.newTitle
      val changesDesc = request.oldDescription != request.newDescription
      if (changesTitle && post.title != request.oldTitle || changesDesc && post.description != request.oldDescription) {
        false
      } else {
        if (changesTitle)
          post.title = request.newTitle
        if (changesDesc)
          post.description = request.newDescription

        request.applied = true
        true
      }
    }
}

case class VotesTagsChangeRequestAccess(
  sign: Long
  ) extends VotesAccessBase[TagChangeRequest] {
    override def nodeDefinition(uuid: String) = FactoryUuidNodeDefinition(TagChangeRequest, uuid)
    override def selectNode(discourse: Discourse) = discourse.tagChangeRequests.head
    override def applyChange(discourse: Discourse, req: TagChangeRequest, post: Post, tx:QueryHandler) = req match {
        case request: AddTags =>
          // we need to get the tag which is connected to the request
          val tagDef = ConcreteFactoryNodeDefinition(Scope)
          val tagsDef = RelationDefinition(nodeDefinition(request.uuid), ProposesTag, tagDef)
          val tag = Discourse(db.queryGraph(Query(s"match ${tagsDef.toQuery} return ${tagDef.name}", tagsDef.parameterMap))).scopes.head
          val tags = Tags.merge(tag, post)
          discourse.add(tag, tags)
          true
        case request: RemoveTags =>
          // we need to get the tag which is connected to the request
          val tagDef = ConcreteFactoryNodeDefinition(Scope)
          val tagsDef = RelationDefinition(tagDef, Tags, ConcreteNodeDefinition(post))
          disconnectHyperNodesFor(tagsDef, tx)
          true
    }
}

case class VotesTagsAccess(sign: Long) extends EndRelationAccessDefault[User, Votes, Votable] {
  val nodeFactory = User

  override def create[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S, Votable with AbstractRelation[S,E], E]) = context.withUser { user =>
    db.transaction { tx =>
      val weight = sign // TODO: karma
      val success = if (weight == 0) {
        val referenceDef = HyperNodeDefinition(FactoryUuidNodeDefinition(Scope, param.startUuid), Tags, FactoryUuidNodeDefinition(Post, param.endUuid))
        val userNode = ConcreteNodeDefinition(user)
        val votes = RelationDefinition(userNode, Votes, referenceDef)
        disconnectNodesFor(votes, tx)
      } else {
        val tag = Scope.matchesOnUuid(param.startUuid)
        val post = Post.matchesOnUuid(param.endUuid)
        val tags = Tags.matches(tag, post)
        val votes = Votes.merge(user, tags, weight = weight, onMatch = Set("weight"))
        val failure = tx.persistChanges(tag, post, tags, votes)
        !failure.isDefined
      }

      Left(if (success)
        Ok(JsObject(Seq(
          ("vote", JsObject(Seq(
            ("weight", JsNumber(weight))
          )))
        )))
      else
        BadRequest("No vote :/")
      )
    }
  }
}

case class VotesConnectsAccess(sign: Long) extends EndRelationAccessDefault[User, Votes, Votable] {
  val nodeFactory = User

  override def create[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S, Votable with AbstractRelation[S,E], E]) = context.withUser { user =>
    db.transaction { tx =>
      val weight = sign // TODO: karma
      val success = if (weight == 0) {
        val referenceDef = HyperNodeDefinition(FactoryUuidNodeDefinition(Connectable, param.startUuid), Connects, FactoryUuidNodeDefinition(Connectable, param.endUuid))
        val userNode = ConcreteNodeDefinition(user)
        val votes = RelationDefinition(userNode, Votes, referenceDef)
        disconnectNodesFor(votes, tx)
      } else {
        val tag = Connectable.matches(uuid = Some(param.startUuid), matches = Set("uuid"))
        val connectable = Connectable.matches(uuid = Some(param.endUuid), matches = Set("uuid"))
        val tags = Connects.matches(tag, connectable)
        val votes = Votes.merge(user, tags, weight = weight, onMatch = Set("weight"))
        val failure = tx.persistChanges(tag, connectable, tags, votes)
        if (failure.isDefined)
        println(failure.get)
        !failure.isDefined
      }

      Left(if (success)
        Ok(JsObject(Seq(
          ("vote", JsObject(Seq(
            ("weight", JsNumber(weight))
          )))
        )))
      else
        BadRequest("No vote :/")
      )
    }
  }
}
