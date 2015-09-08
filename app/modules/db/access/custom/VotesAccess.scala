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
  def applyChange(discourse: Discourse, request: T, post: Post): Boolean

  //TODO: optimize to one request with multiple statements
  override def create(context: RequestContext, param: ConnectParameter[Votable]) = context.withUser { user =>
    db.transaction { tx =>
      // first we match the actual change request and aquire a write lock,
      // which will last for the whole transaction
      val requestDef = nodeDefinition(param.baseUuid)
      val userDef = ConcreteNodeDefinition(user)
      val votesDef = RelationDefinition(userDef, Votes, requestDef)
      val discourse = Discourse(tx.queryGraph(Query(s"match ${requestDef.toQuery}-[:`${UpdatedToPost.relationType}`|`${UpdatedTagsToPost.relationType}`]->(post:`${Post.label}`) set ${requestDef.name}.__lock = true with post,${requestDef.name} optional match ${votesDef.toQuery(true, false)} return post,${requestDef.name}, ${votesDef.name}", votesDef.parameterMap)))
      val request = selectNode(discourse)
      val votes = discourse.votes.headOption
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
        val newVotes = Votes.merge(user, request, weight = weight, onMatch = Set("weight"))
        discourse.add(newVotes)
      }

      val postApplies = if (request.applyVotes >= request.applyThreshold) {
        val post = discourse.posts.head
        request.applied = applyChange(discourse, request, post)
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
            ("votes", JsNumber(request.applyVotes)),
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
    override def applyChange(discourse: Discourse, request: Updated, post: Post) = {
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

case class VotesUpdatedTagsAccess(
  sign: Long
  ) extends VotesAccessBase[UpdatedTags] {
    override def nodeDefinition(uuid: String) = FactoryUuidNodeDefinition(UpdatedTags, uuid)
    override def selectNode(discourse: Discourse) = discourse.updatedTags.head
    override def applyChange(discourse: Discourse, request: UpdatedTags, post: Post) = {
      // we need to get the tag which is connected to the request
      val tagDef = ConcreteFactoryNodeDefinition(TagLike)
      val tagsDef = RelationDefinition(tagDef, Tags, nodeDefinition(request.uuid))
      val tag = Discourse(db.queryGraph(Query(s"match ${tagsDef.toQuery} return ${tagDef.name}", tagsDef.parameterMap))).tagLikes.head
      val tags = Tags.merge(tag, post)
      discourse.add(tag, tags)
      true
    }
}

case class VotesTagsAccess(sign: Long) extends EndRelationAccessDefault[User, Votes, Votable] {
  val nodeFactory = User

  override def create[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S, Votable with AbstractRelation[S,E], E]) = context.withUser { user =>
    db.transaction { tx =>
      val weight = sign // TODO: karma
      val success = if (weight == 0) {
        val referenceDef = HyperNodeDefinition(FactoryUuidNodeDefinition(TagLike, param.startUuid), Tags, FactoryUuidNodeDefinition(Connectable, param.endUuid))
        val userDef = ConcreteNodeDefinition(user)
        val votesDef = RelationDefinition(userDef, Votes, referenceDef)
        // next we define the voting relation between the currently logged in user and the change request
        val userNode = ConcreteNodeDefinition(user)
        val votes = RelationDefinition(userNode, Votes, referenceDef)
        disconnectNodesFor(votes, tx)
      } else {
        val tag = TagLike.matches(uuid = Some(param.startUuid), matches = Set("uuid"))
        val connectable = Connectable.matches(uuid = Some(param.endUuid), matches = Set("uuid"))
        val tags = Tags.matches(tag, connectable)
        val votes = Votes.merge(user, tags, weight = weight, onMatch = Set("weight"))
        val failure = tx.persistChanges(tag, connectable, tags, votes)
        println("VOTEEEEEED")
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

case class VotesConnectsAccess(sign: Long) extends EndRelationAccessDefault[User, Votes, Votable] {
  val nodeFactory = User

  override def create[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S, Votable with AbstractRelation[S,E], E]) = context.withUser { user =>
    db.transaction { tx =>
      val weight = sign // TODO: karma
      val success = if (weight == 0) {
        val referenceDef = HyperNodeDefinition(FactoryUuidNodeDefinition(Connectable, param.startUuid), Connects, FactoryUuidNodeDefinition(Connectable, param.endUuid))
        val userDef = ConcreteNodeDefinition(user)
        val votesDef = RelationDefinition(userDef, Votes, referenceDef)
        // next we define the voting relation between the currently logged in user and the change request
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
