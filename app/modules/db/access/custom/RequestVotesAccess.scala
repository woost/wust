package modules.db.access.custom

import controllers.api.nodes.{HyperConnectParameter, ConnectParameter, RequestContext}
import model.WustSchema.{Created => SchemaCreated, _}
import modules.db.Database._
import modules.db._
import modules.db.helpers.{RequestHelper,PostHelper}
import modules.db.types._
import modules.karma._
import modules.db.access.{EndRelationAccessDefault, EndRelationAccess}
import play.api.libs.json._
import renesca.parameter.implicits._
import renesca.schema._
import renesca._
import play.api.mvc.Results._
import moderation.Moderation

case class ApplyResponse(changed: Boolean, karmaDefinitionOpt: Option[KarmaDefinition])

case class VotesChangeRequestAccess(sign: Long) extends EndRelationAccessDefault[User, Votes, Votable] {

  import formatters.json.EditNodeFormat.PostFormat
  import formatters.json.EditNodeFormat.CRFormat

  val nodeFactory = User

  def requestHelper(request: ChangeRequest): VotesChangeRequestHelper = request match {
    case req: Updated => new VotesUpdatedHelper(req)
    case req: Deleted => new VotesDeletedHelper(req)
    case req: AddTags => new VotesAddTagsHelper(req)
    case req: RemoveTags => new VotesRemoveTagsHelper(req)
  }

  def tryApply(tx: QueryHandler, discourse: Discourse, request: ChangeRequest):Either[String, ApplyResponse] = {
    val helper = requestHelper(request)
    if (request.canApply) {
      if (request.status == PENDING) {
        val success = helper.applyChange(tx, discourse)
        if (success) {
          request.status = APPROVED
          Right(ApplyResponse(true, Some(KarmaDefinition(request.applyThreshold, "Proposed change request approved"))))
        } else Left("Cannot apply changes automatically")
      } else if (request.status == INSTANT) {
        request.status = APPROVED
        Right(ApplyResponse(false, Some(KarmaDefinition(request.applyThreshold, "Instant change request approved"))))
      } else Right(ApplyResponse(false, None))
    } else if (request.canReject) {
      if (request.status == INSTANT) {
        val success = helper.unapplyChange(tx, discourse)
        if (success) {
          request.status = REJECTED
          Right(ApplyResponse(true, Some(KarmaDefinition(-request.applyThreshold, "Instant change request rejected"))))
        } else Left("Cannot unapply changes automatically")
      } else {
        request.status = REJECTED
        Right(ApplyResponse(false, Some(KarmaDefinition(-request.applyThreshold, "Proposed change request rejected"))))
      }
    } else Right(ApplyResponse(false, None))
  }

  override def create(context: RequestContext, param: ConnectParameter[Votable]) = context.withUser { user =>
    db.transaction { tx =>
      // first we match the actual change request and aquire a write lock,
      // which will last for the whole transaction
      implicit val ctx = new QueryContext
      val requestDef = FactoryUuidNodeDef(ChangeRequest, param.baseUuid)
      val postDef = LabelNodeDef[Post](Set.empty)
      val userDef = ConcreteNodeDef(user)
      val votesDef = RelationDef(userDef, Votes, requestDef)
      val createdDef = RelationDef(userDef, SchemaCreated, postDef)

      //TODO: separate queries for subclasses
      //TODO: simpler? locking really needed?
      val query = s"""
      match (user:`${User.label}`)-[updated1:`${Updated.startRelationType}`|`${Deleted.startRelationType}`|`${AddTags.startRelationType}`|`${RemoveTags.startRelationType}`]->${requestDef.toPattern}-[updated2:`${Updated.endRelationType}`|`${Deleted.endRelationType}`|`${AddTags.endRelationType}`|`${RemoveTags.endRelationType}`]->${postDef.toPattern}
      where ${requestDef.name}.status = ${PENDING} or ${requestDef.name}.status = ${INSTANT}
      set ${requestDef.name}._locked = true
      with ${postDef.name}, ${requestDef.name}, updated1, updated2
      optional match (${postDef.name})-[:`${Connects.startRelationType}`|`${Connects.endRelationType}` *0..20]->(connectable: `${Connectable.label}`), ${userDef.toPattern}
      with distinct connectable, ${postDef.name}, ${userDef.name}, ${requestDef.name}, updated1, updated2
      optional match (${userDef.name})-[r:`${HasKarma.relationType}`]->(tag)
      optional match ${votesDef.toPattern(false, false)}
      optional match ${createdDef.toPattern(false,false)}
      return *
      """

      val discourse = Discourse(tx.queryGraph(query, ctx.params))
      discourse.changeRequests.headOption.map { request =>
        val helper = requestHelper(request)

        PostHelper.viewPost(helper.post, user)

        val votes = discourse.votes.headOption
        votes.foreach(request.approvalSum -= _.weight)

        val authorBoost = if (discourse.createds.isEmpty) 0 else Moderation.authorKarmaBoost
        val voteWeight = Moderation.voteWeightFromScopes(discourse.scopes)
        val weight = sign * (voteWeight + authorBoost)

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
          case Right(ApplyResponse(changed, karmaDefinitionOpt)) =>
            request._locked = false
            val failure = tx.persistChanges(discourse)

            failure.map(_ => BadRequest("No vote :/")).getOrElse {
              val nodeResponse = if (changed) Json.toJson(helper.post) else JsNull
              karmaDefinitionOpt.foreach(helper.updateKarma(_))
              Ok(JsObject(Seq(
                ("vote", JsObject(Seq(
                  ("weight", JsNumber(weight))
                ))),
                ("status", JsNumber(request.status)),
                ("votes", JsNumber(request.approvalSum)),
                ("node", nodeResponse),
                ("conflictChangeRequests", Json.toJson(discourse.changeRequests.filter(r => r != request && r.status == CONFLICT)))
              )))
            }
          case Left(err) =>
            //TODO we cannot do anything here, there is no merge conflict resolution
            //we just do not want to show it to the user again in the votestream,
            //so we set it to CONFLICT
            //TODO maybe not rollback, but keep the new voting?
            tx.rollback()
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

trait VotesChangeRequestHelper {
  def post: Post
  def applyChange(tx: QueryHandler, discourse: Discourse): Boolean
  def unapplyChange(tx: QueryHandler, discourse: Discourse): Boolean
  def updateKarma(karmaDefinition: KarmaDefinition): Unit
}

//TODO: we need hyperrelation traits in magic in order to matches on the hyperrelation trait and get correct type: Relation+Node
class VotesUpdatedHelper(request: Updated) extends VotesChangeRequestHelper {

  override def post = request.endNodeOpt.get

  override def unapplyChange(tx: QueryHandler, discourse: Discourse) = {
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
  override def applyChange(tx: QueryHandler, discourse: Discourse) = {
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

  def updateKarma(karmaDefinition: KarmaDefinition) {
    implicit val ctx = new QueryContext
    val postDef = ConcreteNodeDef(request.endNodeOpt.get)
    val userDef = ConcreteNodeDef(request.startNodeOpt.get)

    KarmaUpdate.persistWithConnectedTags(karmaDefinition, KarmaQueryUserPost(userDef, postDef))
  }
}

class VotesDeletedHelper(request: Deleted) extends VotesChangeRequestHelper {

  override def post = request.endNodeOpt.get

  override def unapplyChange(tx: QueryHandler, discourse: Discourse) = {
    if (post.rawItem.labels.contains(Hidden.label)) {
      val hidden = Hidden.wrap(post.rawItem)
      hidden.unhide()
      true
    } else {
      false
    }
  }

  override def applyChange(tx: QueryHandler, discourse: Discourse) = {
    if (!post.rawItem.labels.contains(Hidden.label)) {
      post.hide()
      true
    } else {
      false
    }
  }

  def updateKarma(karmaDefinition: KarmaDefinition) {
    implicit val ctx = new QueryContext
    // match any label, the post might be hidden or a post (maybe better some common label)
    val postDef = LabelUuidNodeDef[Post](UuidNode.labels, request.endNodeOpt.get.uuid)
    val userDef = ConcreteNodeDef(request.startNodeOpt.get)

    KarmaUpdate.persistWithConnectedTagsOfHidden(karmaDefinition, KarmaQueryUserPost(userDef, postDef))
  }
}

trait VotesTagsChangeRequestHelper extends VotesChangeRequestHelper {

  protected val request: TagChangeRequest
  protected def requestToPostDef()(implicit ctx: QueryContext): NodeRelationDef[_ <: TagChangeRequest, _, Post]

  protected def requestGraphUnapply(tx: QueryHandler) = {
    implicit val ctx = new QueryContext
    val tagDef = FactoryNodeDef(Scope)
    val classDef = FactoryNodeDef(Classification)
    val reqDef = ConcreteNodeDef(request)
    val tagsDef = RelationDef(reqDef, ProposesTag, tagDef)
    val classifiesDef = RelationDef(reqDef, ProposesClassify, classDef)

    val query = s"""
    match ${tagsDef.toPattern}
    optional match ${classifiesDef.toPattern(false, true)}
    return ${tagDef.name}, ${classDef.name}
    """

    Discourse(tx.queryGraph(query, ctx.params))
  }

  protected def requestGraphApply(tx: QueryHandler) = {
    implicit val ctx = new QueryContext
    val tagDef = FactoryNodeDef(Scope)
    val classDef = FactoryNodeDef(Classification)
requestToPostDef
    val reqToPostDef = requestToPostDef()
    val reqDef = reqToPostDef.startDefinition

    val tagsDef = RelationDef(reqDef, ProposesTag, tagDef)
    val classifiesDef = RelationDef(reqDef, ProposesClassify, classDef)

    //TODO: locking of other requests?
    val query = s"""
    match ${requestToPostDef.toPattern}, ${tagsDef.toPattern(false, true)}
    where ${reqDef.name}.status = ${PENDING}
    optional match ${classifiesDef.toPattern(false, true)}
    return *
    """

    Discourse(tx.queryGraph(query, ctx.params))
  }

  override def updateKarma(karmaDefinition: KarmaDefinition) {
    implicit val ctx = new QueryContext
    val tagDef = FactoryNodeDef(Scope)
    val tagsDef = RelationDef(ConcreteNodeDef(request), ProposesTag, tagDef)
    //TODO: no hyperrelationtraits in magic...
    val (userNode, postNode) = {
      val hyperRel = request.asInstanceOf[HyperRelation[User, _, _, _, Post]]
      (hyperRel.startNodeOpt.get, hyperRel.endNodeOpt.get)
    }

    val userDef = ConcreteNodeDef(userNode)
    val postDef = ConcreteNodeDef(postNode)

    KarmaUpdate.persistWithProposedTags(karmaDefinition, KarmaQueryUserPost(userDef, postDef), tagsDef)
  }
}

class VotesAddTagsHelper(protected val request: AddTags) extends VotesTagsChangeRequestHelper {

  override def post = request.endNodeOpt.get

  override protected def requestToPostDef()(implicit ctx: QueryContext) = RelationDef(FactoryNodeDef(AddTags), AddTagsEnd, ConcreteNodeDef(post))

  override def unapplyChange(tx: QueryHandler, discourse: Discourse) = {
    val existing = requestGraphUnapply(tx)
    val scope = existing.scopes.head
    val classifications = existing.classifications
    val tags = Tags.matches(scope, post)
    if (classifications.size > 0) {
      discourse.add(tags)
      classifications.foreach { classification =>
        discourse.remove(Classifies.matches(classification, tags))
      }
    } else {
      discourse.remove(tags)
    }

    true
  }

  override def applyChange(tx: QueryHandler, discourse: Discourse) = {
    val existing = requestGraphApply(tx)
    val sameReq = existing.addTags.find(_.uuid == request.uuid).get
    val scope = sameReq.proposesTags.head
    val classifications = sameReq.proposesClassifys
    val tags = Tags.merge(scope, post)
    discourse.add(tags)
    classifications.foreach { classification =>
      discourse.add(Classifies.merge(classification, tags))
    }

    RequestHelper.conflictingAddTags(sameReq, existing.addTags).foreach { cr =>
      cr.status = CONFLICT
      discourse.add(cr)
    }

    true
  }
}

class VotesRemoveTagsHelper(protected val request: RemoveTags) extends VotesTagsChangeRequestHelper {
  override def post = request.endNodeOpt.get

  override protected def requestToPostDef()(implicit ctx: QueryContext) = RelationDef(FactoryNodeDef(RemoveTags), RemoveTagsEnd, ConcreteNodeDef(post))

  override def unapplyChange(tx: QueryHandler, discourse: Discourse) = {
    val existing = requestGraphUnapply(tx)
    val scope = existing.scopes.head
    val classifications = existing.classifications
    val tags = Tags.merge(scope, post)
    if (classifications.size > 0) {
      discourse.add(tags)
      classifications.foreach { classification =>
        discourse.add(Classifies.merge(classification, tags))
      }
    } else {
      discourse.remove(Tags.matches(scope, post))
    }

    true
  }

  override def applyChange(tx: QueryHandler, discourse: Discourse) = {
    val existing = requestGraphApply(tx)
    val sameReq = existing.removeTags.find(_.uuid == request.uuid).get
    val scope = sameReq.proposesTags.head
    val classifications = sameReq.proposesClassifys
    val tags = Tags.matches(scope, post)
    if (classifications.size > 0) {
      discourse.add(tags)
      classifications.foreach { classification =>
        discourse.remove(Classifies.matches(classification, tags))
      }
    } else {
      discourse.remove(tags)
    }

    RequestHelper.conflictingRemoveTags(sameReq, existing.removeTags).foreach { cr =>
      cr.status = CONFLICT
      discourse.add(cr)
    }

    true
  }
}
