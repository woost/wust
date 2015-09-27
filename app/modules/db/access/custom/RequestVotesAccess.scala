package modules.db.access.custom

import controllers.api.nodes.{HyperConnectParameter, ConnectParameter, RequestContext}
import model.WustSchema.{Created => SchemaCreated, _}
import modules.db.Database._
import modules.db._
import modules.karma._
import modules.db.access.{EndRelationAccessDefault, EndRelationAccess}
import play.api.libs.json._
import renesca.parameter.implicits._
import renesca.schema._
import renesca._
import play.api.mvc.Results._
import moderation.Moderation

trait ChangeRequestHelper[T <: ChangeRequest] {
  def applyChange(tx: QueryHandler, discourse: Discourse, request: T, post: Post): Boolean
  def unapplyChange(tx: QueryHandler, discourse: Discourse, request: T, post: Post): Boolean
  def updateKarma(request: T, karmaDefinition: KarmaDefinition): Unit
}

case class VotesChangeRequestAccess(sign: Long) extends EndRelationAccessDefault[User, Votes, Votable] {

  import formatters.json.EditNodeFormat.PostFormat

  val nodeFactory = User

  def requestHelper[T <: ChangeRequest](request: T): ChangeRequestHelper[T] = request match {
    //TODO: no asInstanceOf?
    case req: Updated => VotesUpdatedHelper.asInstanceOf[ChangeRequestHelper[T]]
    case req: Deleted => VotesDeletedHelper.asInstanceOf[ChangeRequestHelper[T]]
    case req: TagChangeRequest => VotesTagsChangeRequestHelper.asInstanceOf[ChangeRequestHelper[T]]
  }

  def tryApply(tx: QueryHandler, discourse: Discourse, request: ChangeRequest):Either[String,(Option[Post], Option[KarmaDefinition])] = {
    val helper = requestHelper(request)
    if (request.canApply) {
      if (request.status == PENDING) {
        val post = discourse.posts.head
        val success = helper.applyChange(tx, discourse, request, post)

        if (success) {
          request.status = APPROVED
          Right((Some(post), Some(KarmaDefinition(request.applyThreshold, "Proposed change request approved"))))
        } else {
          Left("Cannot apply changes automatically")
        }
      } else if (request.status == INSTANT) {
        request.status = APPROVED
        Right((None, Some(KarmaDefinition(request.applyThreshold, "Instant change request approved"))))
      } else Right((None, None))
    } else if (request.canReject) {
      if (request.status == INSTANT) {
        val post = discourse.posts.headOption.getOrElse(Post.wrap(discourse.hiddens.head.rawItem))
        val success = helper.unapplyChange(tx, discourse, request, post)
        if (success) {
          request.status = REJECTED
          Right((Some(post), Some(KarmaDefinition(-request.applyThreshold, "Instant change request rejected"))))
        } else {
          Left("Cannot unapply changes automatically")
        }
      } else {
        request.status = REJECTED
        Right((None, Some(KarmaDefinition(-request.applyThreshold, "Proposed change request rejected"))))
      }
    } else Right((None, None))
  }

  override def create(context: RequestContext, param: ConnectParameter[Votable]) = context.withUser { user =>
    db.transaction { tx =>
      // first we match the actual change request and aquire a write lock,
      // which will last for the whole transaction
      implicit val ctx = new QueryContext
      val requestDef = FactoryUuidNodeDefinition(ChangeRequest, param.baseUuid)
      val postDef = LabelNodeDefinition[Post](Set.empty)
      val userDef = ConcreteNodeDefinition(user)
      val votesDef = RelationDefinition(userDef, Votes, requestDef)
      val createdDef = RelationDefinition(userDef, SchemaCreated, postDef)

      //TODO: separate queries for subclasses
      //TODO: simpler? locking really needed?
      val query = s"""
      match (user:`${User.label}`)-[updated1:`${Updated.startRelationType}`|`${Deleted.startRelationType}`|`${AddTags.startRelationType}`|`${RemoveTags.startRelationType}`]->${requestDef.toQuery}-[updated2:`${Updated.endRelationType}`|`${Deleted.endRelationType}`|`${AddTags.endRelationType}`|`${RemoveTags.endRelationType}`]->${postDef.toQuery}
      where ${requestDef.name}.status = ${PENDING} or ${requestDef.name}.status = ${INSTANT}
      set ${requestDef.name}._locked = true
      with ${postDef.name}, ${requestDef.name}, updated1, updated2
      optional match (${postDef.name})-[:`${Connects.startRelationType}`|`${Connects.endRelationType}` *0..20]->(connectable: `${Connectable.label}`), ${userDef.toQuery}
      with distinct connectable, ${postDef.name}, ${userDef.name}, ${requestDef.name}, updated1, updated2
      optional match (${userDef.name})-[r:`${HasKarma.relationType}`]->(tag)
      optional match ${votesDef.toQuery(false, false)}
      optional match ${createdDef.toQuery(false,false)}
      return *
      """

      val discourse = Discourse(tx.queryGraph(query, createdDef.parameterMap ++ votesDef.parameterMap))
      discourse.changeRequests.headOption.map { request =>
        val helper = requestHelper(request)
        val votes = discourse.votes.headOption
        votes.foreach(request.approvalSum -= _.weight)

        val authorBoost = if (discourse.createds.isEmpty) 0 else Moderation.authorKarmaBoost

        val karma = Moderation.voteWeightFromScopes(discourse.scopes)
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

        val postApplies = tryApply(tx, discourse, request)

        postApplies match {
          case Right((nodeOpt, karmaDefinitionOpt)) =>
            request._locked = false
            val failure = tx.persistChanges(discourse)

            failure.map(_ => BadRequest("No vote :/")).getOrElse {
              karmaDefinitionOpt.foreach(helper.updateKarma(request, _))
              Ok(JsObject(Seq(
                ("vote", JsObject(Seq(
                  ("weight", JsNumber(weight))
                ))),
                ("status", JsNumber(request.status)),
                ("votes", JsNumber(request.approvalSum)),
                ("node", nodeOpt.map(Json.toJson(_)).getOrElse(JsNull))
              )))
            }
          case Left(err) =>
            tx.rollback()
            //TODO we cannot do anything here, there is no merge conflict resolution
            //we just do not want to show it to the user again in the votestream,
            //so we set it to CONFLICT
            db.transaction { tx =>
              val request = ChangeRequest.matchesOnUuid(param.baseUuid)
              request.status = CONFLICT
              tx.persistChanges(request)
            }
            BadRequest(err)
        }
      }
    } getOrElse {
      BadRequest("Cannot vote on outdated request")
    }
  }
}

//TODO: we need hyperrelation traits in magic in order to matches on the hyperrelation trait and get correct type: Relation+Node
object VotesUpdatedHelper extends ChangeRequestHelper[Updated] {

  override def unapplyChange(tx: QueryHandler, discourse: Discourse, request: Updated, post: Post) = {
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
  override def applyChange(tx: QueryHandler, discourse: Discourse, request: Updated, post: Post) = {
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

  def updateKarma(request: Updated, karmaDefinition: KarmaDefinition) {
    implicit val ctx = new QueryContext
    val postDef = ConcreteNodeDefinition(request.endNodeOpt.get)
    val userDef = ConcreteNodeDefinition(request.startNodeOpt.get)

    val query = s"match ${userDef.toQuery}, ${postDef.toQuery}"
    val params = postDef.parameterMap ++ userDef.parameterMap

    val karmaQuery = KarmaQuery(postDef, userDef, query, params)
    KarmaUpdate.persistWithConnectedTags(karmaDefinition, karmaQuery)
  }
}

object VotesDeletedHelper extends ChangeRequestHelper[Deleted] {

  override def unapplyChange(tx: QueryHandler, discourse: Discourse, request: Deleted, post: Post) = {
    if (post.rawItem.labels.contains(Hidden.label)) {
      val hidden = Hidden.wrap(post.rawItem)
      hidden.unhide()
      true
    } else {
      false
    }
  }

  override def applyChange(tx: QueryHandler, discourse: Discourse, request: Deleted, post: Post) = {
    if (!post.rawItem.labels.contains(Hidden.label)) {
      post.hide()
      true
    } else {
      false
    }
  }

  def updateKarma(request: Deleted, karmaDefinition: KarmaDefinition) {
    implicit val ctx = new QueryContext
    // match any label, the post might be hidden or a post (maybe better some common label)
    val postDef = LabelUuidNodeDefinition[Post](UuidNode.labels, request.endNodeOpt.get.uuid)
    val userDef = ConcreteNodeDefinition(request.startNodeOpt.get)

    val query = s"match ${userDef.toQuery}, ${postDef.toQuery}"
    val params = postDef.parameterMap ++ userDef.parameterMap

    val karmaQuery = KarmaQuery(postDef, userDef, query, params)
    KarmaUpdate.persistWithConnectedTagsOfHidden(karmaDefinition, karmaQuery)
  }
}

object VotesTagsChangeRequestHelper extends ChangeRequestHelper[TagChangeRequest] {

  override def unapplyChange(tx: QueryHandler, discourse: Discourse, req: TagChangeRequest, post: Post) = {
    implicit val ctx = new QueryContext
    val tagDef = ConcreteFactoryNodeDefinition(Scope)
    val tagsDef = RelationDefinition(ConcreteNodeDefinition(req), ProposesTag, tagDef)
    val scope = Discourse(tx.queryGraph(Query(s"match ${tagsDef.toQuery} return ${tagDef.name}", tagsDef.parameterMap))).scopes.head
    req match {
      case request: AddTags =>
        discourse.remove(Tags.matches(scope, post))
      case request: RemoveTags =>
        discourse.add(Tags.merge(scope, post))
    }

    true
  }

  override def applyChange(tx: QueryHandler, discourse: Discourse, req: TagChangeRequest, post: Post) = {
    // we need to get the tag which is connected to the request
    // TODO: resolve matches startnode via relation in renesca?
    implicit val ctx = new QueryContext
    val tagDef = ConcreteFactoryNodeDefinition(Scope)
    val tagsDef = RelationDefinition(ConcreteNodeDefinition(req), ProposesTag, tagDef)
    val scope = Discourse(tx.queryGraph(Query(s"match ${tagsDef.toQuery} return ${tagDef.name}", tagsDef.parameterMap))).scopes.head
    req match {
      case request: AddTags =>
        discourse.add(Tags.merge(scope, post))
      case request: RemoveTags =>
        discourse.remove(Tags.matches(scope, post))
    }

    true
  }

  override def updateKarma(req: TagChangeRequest, karmaDefinition: KarmaDefinition) {
    implicit val ctx = new QueryContext
    val tagDef = ConcreteFactoryNodeDefinition(Scope)
    val tagsDef = RelationDefinition(ConcreteNodeDefinition(req), ProposesTag, tagDef)
    val (userNode, postNode) = req match {
      case request: AddTags => (request.startNodeOpt.get, request.endNodeOpt.get)
      case request: RemoveTags => (request.startNodeOpt.get, request.endNodeOpt.get)
    }

    val userDef = ConcreteNodeDefinition(userNode)
    val postDef = ConcreteNodeDefinition(postNode)

    val query = s"match ${userDef.toQuery}, ${postDef.toQuery}"
    val params = userDef.parameterMap ++ postDef.parameterMap

    val karmaQuery = KarmaQuery(postDef, userDef, query, params)
    KarmaUpdate.persistWithProposedTags(karmaDefinition, karmaQuery, tagsDef)
  }
}

