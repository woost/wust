package modules.db.access.custom

import controllers.api.nodes.{HyperConnectParameter, ConnectParameter, RequestContext}
import model.WustSchema.{Created => SchemaCreated, _}
import modules.db.Database._
import modules.db._
import modules.db.access.{EndRelationAccessDefault, EndRelationAccess}
import play.api.libs.json._
import renesca.parameter.implicits._
import renesca.schema._
import renesca._
import play.api.mvc.Results._
import moderation.Moderation

trait VotesChangeRequestAccess[T <: ChangeRequest] extends EndRelationAccessDefault[User, Votes, Votable] {

  import formatters.json.EditNodeFormat.PostFormat

  val sign: Long
  val nodeFactory = User

  def nodeDefinition(uuid: String): FactoryUuidNodeDefinition[T]
  def selectNode(discourse: Discourse): T
  def applyChange(discourse: Discourse, request: T, post: Post, tx:QueryHandler): Boolean
  def unapplyChange(discourse: Discourse, request: T, post: Post, tx: QueryHandler): Boolean
  def updateKarma(tx: QueryHandler, request: T, karma: Long): Unit

  override def create(context: RequestContext, param: ConnectParameter[Votable]) = context.withUser { user =>
    db.transaction { tx =>
      // first we match the actual change request and aquire a write lock,
      // which will last for the whole transaction
      val requestDef = nodeDefinition(param.baseUuid)
      val postDef = LabelNodeDefinition[Post](Set.empty)
      val userDef = ConcreteNodeDefinition(user)
      val votesDef = RelationDefinition(userDef, Votes, requestDef)
      val createdDef = RelationDefinition(userDef, SchemaCreated, postDef)

      //TODO: separate queries for subclasses
      val query = s"""
      match (user:`${User.label}`)-[updated1:`${UserToUpdated.relationType}`|`${UserToDeleted.relationType}`|`${UserToAddTags.relationType}`|`${UserToRemoveTags.relationType}`]->${requestDef.toQuery}-[updated2:`${UpdatedToPost.relationType}`|`${DeletedToHidden.relationType}`|`${AddTagsToPost.relationType}`|`${RemoveTagsToPost.relationType}`]->${postDef.toQuery}, ${userDef.toQuery}
      where ${requestDef.name}.applied = ${PENDING} or ${requestDef.name}.applied = ${INSTANT}
      set ${requestDef.name}._locked = true
      with ${postDef.name},${userDef.name},${requestDef.name}, updated1, updated2
      optional match ${votesDef.toQuery(false, false)}
      optional match ${createdDef.toQuery(false,false)}
      return *
      """

      val discourse = Discourse(tx.queryGraph(query, createdDef.parameterMap ++ votesDef.parameterMap))
      val request = selectNode(discourse)

      val votes = discourse.votes.headOption
      votes.foreach(request.approvalSum -= _.weight)

      val karma = 1 // TODO karma
      val authorBoost = if (discourse.createds.isEmpty) 0 else Moderation.authorKarmaBoost
      val weight = sign * (karma + authorBoost)
      if (weight == 0) {
        // if there are any existing votes, disconnect them
        votes.foreach(discourse.remove(_))
      } else {
        // we want to vote on the change request with our weight. we merge the
        // votes relation as we want to override any previous vote. merging is
        // better than just updating the weight on an existing relation, as it
        // guarantees uniqueness
        request.approvalSum += weight
        val newVotes = Votes.merge(user, request, weight = weight, onMatch = Set("weight"))
        discourse.add(newVotes)
      }

      val postApplies = if (request.applied == PENDING && request.canApply) {
        // delete request are never pending, so it is ok to search for posts instead of hidden
        val post = discourse.posts.head
        val success = applyChange(discourse, request, post, tx)

        if (success) {
          request.applied = APPLIED
          updateKarma(tx, request, request.applyThreshold)
          Right(Some(post))
        } else
          Left("Cannot apply changes automatically")
      } else if (request.canReject) {
        if (request.applied == INSTANT) {
          val post = discourse.posts.headOption.getOrElse(Post.wrap(discourse.hiddens.head.rawItem))
          val success = unapplyChange(discourse, request, post, tx)
          if (success) {
            request.applied = REJECTED
            updateKarma(tx, request, -request.applyThreshold)
            Right(Some(post))
          } else {
            Left("Cannot unapply changes automatically")
          }
        } else {
          request.applied = REJECTED
          Right(None)
        }
      } else {
          Right(None)
      }

      postApplies match {
        case Right(nodeOpt) =>
          request._locked = false
          val failure = tx.persistChanges(discourse)

          failure.map(_ => BadRequest("No vote :/")).getOrElse {
            Ok(JsObject(Seq(
              ("vote", JsObject(Seq(
                ("weight", JsNumber(weight))
              ))),
              ("applied", JsNumber(request.applied)),
              ("votes", JsNumber(request.approvalSum)),
              ("node", nodeOpt.map(Json.toJson(_)).getOrElse(JsNull))
            )))
          }
        case Left(err) =>
          tx.rollback()
          //TODO we cannot do anything here, there is no merge conflict resolution
          //we just do not want to show it to the user again in the votestream,
          //so we skip the change request
          db.transaction { tx =>
            val request = ChangeRequest.matchesOnUuid(param.baseUuid)
            tx.persistChanges(Skipped.merge(user, request))
          }
          BadRequest(err)
      }
    }
  }
}

//TODO: we need hyperrelation traits in magic in order to matches on the hyperrelation trait and get correct type: Relation+Node
case class VotesUpdatedAccess(
  sign: Long
  ) extends VotesChangeRequestAccess[Updated] {

  override def nodeDefinition(uuid: String) = FactoryUuidNodeDefinition(Updated, uuid)
  override def selectNode(discourse: Discourse) = discourse.updateds.head
  override def unapplyChange(discourse: Discourse, request: Updated, post: Post, tx:QueryHandler) = {
    val changesTitle = request.oldTitle != request.newTitle
    val changesDesc = request.oldDescription != request.newDescription
    if (changesTitle && post.title != request.newTitle || changesDesc && post.description != request.newDescription) {
      false
    } else {
      if (changesTitle)
        post.title = request.oldTitle
      if (changesDesc)
        post.description = request.oldDescription

      true
    }
  }
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

      true
    }
  }

  def updateKarma(tx: QueryHandler, request: Updated, karma: Long) {
    val postDef = ConcreteNodeDefinition(request.endNodeOpt.get)
    val userDef = ConcreteNodeDefinition(request.startNodeOpt.get)

    val query = s"""
    match ${userDef.toQuery},
    ${postDef.toQuery}-[:`${Connects.startRelationType}`|`${Connects.endRelationType}` *0..20]->(connectable: `${Connectable.label}`)
    with distinct connectable, ${userDef.name}
    match (tag: `${Scope.label}`)-[:`${Tags.startRelationType}`]->(:`${Tags.label}`)-[:`${Tags.endRelationType}`]->(connectable: `${Post.label}`)
    merge (${userDef.name})-[r:`${HasKarma.relationType}`]->(tag)
    on create set r.karma = {karma}
    on match set r.karma = r.karma + {karma}
    """

    val params = postDef.parameterMap ++ userDef.parameterMap ++ Map("karma" -> karma)

    tx.query(query, params)
  }
}

case class VotesDeletedAccess(
  sign: Long
  ) extends VotesChangeRequestAccess[Deleted] {

  override def nodeDefinition(uuid: String) = FactoryUuidNodeDefinition(Deleted, uuid)
  override def selectNode(discourse: Discourse) = discourse.deleteds.head
  override def unapplyChange(discourse: Discourse, request: Deleted, post: Post, tx:QueryHandler) = {
    if (post.rawItem.labels.contains(Hidden.label)) {
      val hidden = Hidden.wrap(post.rawItem)
      hidden.unhide()
      true
    } else {
      false
    }
  }
  // we do not have noninstant delete changes, so no applychanges in voting
  override def applyChange(discourse: Discourse, request: Deleted, post: Post, tx:QueryHandler) = ???

  def updateKarma(tx: QueryHandler, request: Deleted, karma: Long) {
    val postDef = ConcreteNodeDefinition(request.endNodeOpt.get)
    val userDef = ConcreteNodeDefinition(request.startNodeOpt.get)

    val query = s"""
    match ${userDef.toQuery},
    ${postDef.toQuery}-[:`${Connects.startRelationType}`|`${Connects.endRelationType}` *0..20]->(connectable: `${Connectable.label}`)
    with distinct connectable, ${userDef.name}
    match (tag: `${Scope.label}`)-[:`${Tags.startRelationType}`]->(:`${Tags.label}`)-[:`${Tags.endRelationType}`]->(connectable: `${Post.label}`)
    merge (${userDef.name})-[r:`${HasKarma.relationType}`]->(tag)
    on create set r.karma = {karma}
    on match set r.karma = r.karma + {karma}
    """

    val params = postDef.parameterMap ++ userDef.parameterMap ++ Map("karma" -> karma)

    tx.query(query, params)
  }
}

case class VotesTagsChangeRequestAccess(
  sign: Long
  ) extends VotesChangeRequestAccess[TagChangeRequest] {

  override def nodeDefinition(uuid: String) = FactoryUuidNodeDefinition(TagChangeRequest, uuid)
  override def selectNode(discourse: Discourse) = discourse.tagChangeRequests.head

  override def unapplyChange(discourse: Discourse, req: TagChangeRequest, post: Post, tx:QueryHandler) = {
    val tagDef = ConcreteFactoryNodeDefinition(Scope)
    val tagsDef = RelationDefinition(nodeDefinition(req.uuid), ProposesTag, tagDef)
    val scope = Discourse(tx.queryGraph(Query(s"match ${tagsDef.toQuery} return ${tagDef.name}", tagsDef.parameterMap))).scopes.head
    req match {
      case request: AddTags =>
        discourse.remove(Tags.matches(scope, post))
      case request: RemoveTags =>
        discourse.add(Tags.merge(scope, post))
    }

    true
  }

  override def applyChange(discourse: Discourse, req: TagChangeRequest, post: Post, tx:QueryHandler) = {
    // we need to get the tag which is connected to the request
    // TODO: resolve matches startnode via relation in renesca?
    val tagDef = ConcreteFactoryNodeDefinition(Scope)
    val tagsDef = RelationDefinition(nodeDefinition(req.uuid), ProposesTag, tagDef)
    val scope = Discourse(tx.queryGraph(Query(s"match ${tagsDef.toQuery} return ${tagDef.name}", tagsDef.parameterMap))).scopes.head
    req match {
      case request: AddTags =>
        discourse.add(Tags.merge(scope, post))
      case request: RemoveTags =>
        discourse.remove(Tags.matches(scope, post))
    }

    true
  }

  override def updateKarma(tx: QueryHandler, req: TagChangeRequest, karma: Long) {
    val tagDef = ConcreteFactoryNodeDefinition(Scope)
    val tagsDef = RelationDefinition(nodeDefinition(req.uuid), ProposesTag, tagDef)
    val userDef = req match {
      case request: AddTags =>
        ConcreteNodeDefinition(request.startNodeOpt.get)
      case request: RemoveTags =>
        ConcreteNodeDefinition(request.startNodeOpt.get)
    }

    val query = s"""
    match ${userDef.toQuery}, ${tagsDef.toQuery}
    merge (${userDef.name})-[r:`${HasKarma.relationType}`]->(${tagDef.name})
    on create set r.karma = {karma}
    on match set r.karma = r.karma + {karma}
    """

    val params = tagsDef.parameterMap ++ userDef.parameterMap ++ Map("karma" -> karma)

    tx.query(query, params)
  }
}

