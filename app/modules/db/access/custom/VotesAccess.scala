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

  //TODO: optimize to one request with multiple statements
  override def create(context: RequestContext, param: ConnectParameter[Votable]) = context.withUser { user =>
    db.transaction { tx =>
      // first we match the actual change request and aquire a write lock,
      // which will last for the whole transaction
      val requestDef = nodeDefinition(param.baseUuid)
      val postDef = LabelNodeDefinition[Post](Set.empty)
      val userDef = ConcreteNodeDefinition(user)
      val votesDef = RelationDefinition(userDef, Votes, requestDef)
      val createdDef = RelationDefinition(userDef, SchemaCreated, postDef)

      val query = s"""
      match ${requestDef.toQuery}-[:`${UpdatedToPost.relationType}`|`${DeletedToHidden.relationType}`|`${AddTagsToPost.relationType}`|`${RemoveTagsToPost.relationType}`]->${postDef.toQuery}, ${userDef.toQuery}
      where ${requestDef.name}.applied = ${PENDING} or ${requestDef.name}.applied = ${INSTANT}
      set ${requestDef.name}._locked = true
      with ${postDef.name},${userDef.name},${requestDef.name}
      optional match ${votesDef.toQuery(false, false)}
      optional match ${createdDef.toQuery(false,false)}
      return ${createdDef.name},${postDef.name},${requestDef.name},${votesDef.name}
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

      val postApplies = if (request.applied == PENDING && request.canApply) {
        // delete request are never pending, so it is ok to search for posts instead of hidden
        val post = discourse.posts.head
        val success = applyChange(discourse, request, post, tx)

        if (success) {
          request.applied = APPLIED
          Right(Some(post))
        } else
          Left("Cannot apply changes automatically")
      } else if (request.canReject) {
        if (request.applied == INSTANT) {
          val post = discourse.posts.headOption.getOrElse(Post.wrap(discourse.hiddens.head.rawItem))
          val success = unapplyChange(discourse, request, post, tx)
          if (success) {
            request.applied = REJECTED
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
}

trait VotesReferenceAccess[T <: Reference] extends EndRelationAccessDefault[User, Votes, Votable] {
  val sign: Long
  val nodeFactory = User

  def nodeDefinition(startUuid: String, endUuid: String): HyperNodeDefinitionBase[T]
  def selectNode(discourse: Discourse, startUuid: String, endUuid: String): T
  def selectPost(reference: T): Post
  def updateKarma(tx: QueryHandler, reference: T, karma: Long): Unit

  override def create[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S, Votable with AbstractRelation[S,E], E]) = context.withUser { user =>
    db.transaction { tx =>
      val weight = sign // TODO: karma
      val referenceDef = nodeDefinition(param.startUuid, param.endUuid)
      val userNode = ConcreteNodeDefinition(user)
      val votesDef = RelationDefinition(userNode, Votes, referenceDef)

      val query = s"""
      match ${referenceDef.toQuery}
      set ${referenceDef.name}._locked = true
      with ${referenceDef.startName},${referenceDef.startRelationName},${referenceDef.endName},${referenceDef.endRelationName},${referenceDef.name}
      optional match ${votesDef.toQuery(true, false)} return *
      """

      val discourse = Discourse(tx.queryGraph(Query(query, votesDef.parameterMap)))
      val reference = selectNode(discourse, param.startUuid, param.endUuid)
      val votes = discourse.votes.headOption
      votes.foreach(reference.voteCount -= _.weight)

      val success = if (weight == 0) {
        // if there are any existing votes, disconnect them
        votes.foreach { vote =>
          updateKarma(tx, reference, -vote.weight)
          discourse.remove(vote)
        }
        true
      } else {
        // we want to vote on the change request with our weight. we merge the
        // votes relation as we want to override any previous vote. merging is
        // better than just updating the weight on an existing relation, as it
        // guarantees uniqueness
        reference.voteCount += weight
        val newVotes = Votes.merge(user, reference, weight = weight, onMatch = Set("weight"))
        discourse.add(newVotes)
        updateKarma(tx, reference, weight)
      }

      val post = selectPost(reference)
      val quality = reference.quality(post.viewCount)

      reference._locked = false
      val failure = tx.persistChanges(discourse)
      if (failure.isEmpty) {
        Ok(JsObject(Seq(
          ("quality", JsNumber(quality)),
          ("vote", JsObject(Seq(
            ("weight", JsNumber(weight))
          )))
        )))
      } else {
        BadRequest("No vote :/")
      }
    }
  }
}

case class VotesTagsAccess(sign: Long) extends VotesReferenceAccess[Tags] {

  override def selectNode(discourse: Discourse, startUuid: String, endUuid: String) = discourse.tags.find(t => t.startNodeOpt.map(_.uuid == startUuid).getOrElse(false) && t.endNodeOpt.map(_.uuid == endUuid).getOrElse(false)).get

  override def nodeDefinition(startUuid: String, endUuid: String): HyperNodeDefinitionBase[Tags] = {
    HyperNodeDefinition(FactoryUuidNodeDefinition(Scope, startUuid), Tags, FactoryUuidNodeDefinition(Post, endUuid))
  }

  override def selectPost(reference: Tags) = reference.endNodeOpt.get

  override def updateKarma(tx: QueryHandler, reference: Tags, karma: Long) {
    val tagDef = ConcreteNodeDefinition(reference.startNodeOpt.get)
    val postDef = ConcreteNodeDefinition(reference.endNodeOpt.get)
    val userDef = ConcreteFactoryNodeDefinition(User)
    val createdDef = RelationDefinition(userDef, SchemaCreated, postDef)

    val query = s"""
    match ${createdDef.toQuery}, ${tagDef.toQuery}
    merge (${userDef.name})-[r:`${HasKarma.relationType}`]->(${tagDef.name})
    on create set r.karma = {karma}
    on match set r.karma = r.karma + {karma}
    """

    val params = tagDef.parameterMap ++ createdDef.parameterMap ++ Map("karma" -> karma)

    tx.query(query, params)
  }
}

case class VotesConnectsAccess(sign: Long) extends VotesReferenceAccess[Connects] {

  override def selectNode(discourse: Discourse, startUuid: String, endUuid: String) = discourse.connects.find(c => c.startNodeOpt.map(_.uuid == startUuid).getOrElse(false) && c.endNodeOpt.map(_.uuid == endUuid).getOrElse(false)).get

  override def nodeDefinition(startUuid: String, endUuid: String): HyperNodeDefinitionBase[Connects] = {
    HyperNodeDefinition(FactoryUuidNodeDefinition(Post, startUuid), Connects, FactoryUuidNodeDefinition(Connectable, endUuid))
  }

  override def selectPost(reference: Connects) = reference.startNodeOpt.get

  override def updateKarma(tx: QueryHandler, reference: Connects, karma: Long) {
    val connDef = ConcreteNodeDefinition(reference.endNodeOpt.get)
    val postDef = ConcreteNodeDefinition(reference.startNodeOpt.get)
    val userDef = ConcreteFactoryNodeDefinition(User)
    val createdDef = RelationDefinition(userDef, SchemaCreated, postDef)

    val query = s"""
    match ${createdDef.toQuery}
    ${connDef.toQuery}-[:`${Connects.startRelationType}`|`${Connects.endRelationType}` *0..20]->(connectable: `${Connectable.label}`),
    (tag: `${Scope.label}`)-[:`${Tags.startRelationType}`]->(:`${Tags.label}`)-[:`${Tags.endRelationType}`]->(connectable: `${Post.label}`)
    merge (${userDef.name})-[r:`${HasKarma.relationType}`]->(tag)
    on create set r.karma = {karma}
    on match set r.karma = r.karma + {karma}
    """

    val params = connDef.parameterMap ++ createdDef.parameterMap ++ Map("karma" -> karma)

    tx.query(query, params)
  }
}
