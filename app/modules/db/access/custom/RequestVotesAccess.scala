package modules.db.access.custom

import controllers.api.nodes.{HyperConnectParameter, ConnectParameter, RequestContext}
import model.WustSchema.{Created => SchemaCreated, _}
import modules.db.Database._
import modules.db._
import modules.db.types._
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
  import formatters.json.EditNodeFormat.CRFormat

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
                ("node", nodeOpt.map(Json.toJson(_)).getOrElse(JsNull)),
                ("conflictChangeRequests", Json.toJson(discourse.changeRequests.filter(r => r != request && r.status == CONFLICT)))
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

    KarmaUpdate.persistWithConnectedTags(karmaDefinition, KarmaQueryUserPost(userDef, postDef))
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

    KarmaUpdate.persistWithConnectedTagsOfHidden(karmaDefinition, KarmaQueryUserPost(userDef, postDef))
  }
}

object VotesTagsChangeRequestHelper extends ChangeRequestHelper[TagChangeRequest] {
  private def requestGraphUnapply(tx: QueryHandler, req: TagChangeRequest) = {
    implicit val ctx = new QueryContext
    val tagDef = ConcreteFactoryNodeDefinition(Scope)
    val classDef = ConcreteFactoryNodeDefinition(Classification)
    val reqDef = ConcreteNodeDefinition(req)
    val tagsDef = RelationDefinition(reqDef, ProposesTag, tagDef)
    val classifiesDef = RelationDefinition(reqDef, ProposesClassify, classDef)

    val query = s"""
    match ${tagsDef.toQuery}
    optional match ${classifiesDef.toQuery(false, true)}
    return ${tagDef.name}, ${classDef.name}
    """

    Discourse(tx.queryGraph(query, tagsDef.parameterMap))
  }

  private def requestGraphApply(tx: QueryHandler, req: TagChangeRequest, post: Post) = {
    implicit val ctx = new QueryContext
    val tagDef = ConcreteFactoryNodeDefinition(Scope)
    val classDef = ConcreteFactoryNodeDefinition(Classification)
    val (reqDef: NodeDefinition[TagChangeRequest], reqToPostDef: NodeRelationDefinition[_,_,_]) = req match {
      case _ :AddTags =>
        val reqDef = ConcreteFactoryNodeDefinition(AddTags)
        val relDef = RelationDefinition(reqDef, AddTagsEnd, ConcreteNodeDefinition(post))
        (reqDef, relDef)
      case _ :RemoveTags =>
        val reqDef = ConcreteFactoryNodeDefinition(RemoveTags)
        val relDef = RelationDefinition(reqDef, RemoveTagsEnd, ConcreteNodeDefinition(post))
        (reqDef, relDef)
    }

    val tagsDef = RelationDefinition(reqDef, ProposesTag, tagDef)
    val classifiesDef = RelationDefinition(reqDef, ProposesClassify, classDef)

    //TODO: locking of other requests?
    val query = s"""
    match ${reqToPostDef.toQuery}, ${tagsDef.toQuery(false, true)}
    where ${reqDef.name}.status = ${PENDING}
    optional match ${classifiesDef.toQuery(false, true)}
    return *
    """

    Discourse(tx.queryGraph(query, reqToPostDef.parameterMap ++ tagsDef.parameterMap ++ classifiesDef.parameterMap))
  }

  override def unapplyChange(tx: QueryHandler, discourse: Discourse, req: TagChangeRequest, post: Post) = {
    val existing = requestGraphUnapply(tx, req)
    val scope = existing.scopes.head
    val classifications = existing.classifications

    req match {
      case request: AddTags =>
        val tags = Tags.matches(scope, post)
        if (classifications.size > 0) {
          discourse.add(tags)
          classifications.foreach { classification =>
            discourse.remove(Classifies.matches(classification, tags))
          }
        } else {
          discourse.remove(tags)
        }
      case request: RemoveTags =>
        val tags = Tags.merge(scope, post)
        if (classifications.size > 0) {
          discourse.add(tags)
          classifications.foreach { classification =>
            discourse.add(Classifies.merge(classification, tags))
          }
        } else {
          discourse.remove(Tags.matches(scope, post))
        }
    }

    true
  }

  override def applyChange(tx: QueryHandler, discourse: Discourse, req: TagChangeRequest, post: Post) = {
    val existing = requestGraphApply(tx, req, post)
    val sameReq = existing.tagChangeRequests.find(_.uuid == req.uuid).get
    val (scope, classifications) = sameReq match {
      case addTag: AddTags => (addTag.proposesTags.head, addTag.proposesClassifys)
      case remTag: RemoveTags => (remTag.proposesTags.head, remTag.proposesClassifys)
    }

    req match {
      case request: AddTags =>
        val tags = Tags.merge(scope, post)
        discourse.add(tags)
        classifications.foreach { classification =>
          discourse.add(Classifies.merge(classification, tags))
        }

        existing.addTags.filter(_.uuid != request.uuid).filter { addTag =>
          addTag.proposesTags.head.uuid == scope.uuid && addTag.proposesClassifys.toSet.subsetOf(classifications.toSet)
        }.foreach { cr =>
          cr.status = CONFLICT
          discourse.add(cr)
        }
      case request: RemoveTags =>
        //TODO: delete relation if those were the only classifications?
        val tags = Tags.matches(scope, post)
        if (classifications.size > 0) {
          discourse.add(tags)
          classifications.foreach { classification =>
            discourse.remove(Classifies.matches(classification, tags))
          }
        } else {
          discourse.remove(tags)
        }

        existing.removeTags.filter(_.uuid != request.uuid).filter { remTag =>
          remTag.proposesTags.head.uuid == scope.uuid && (classifications.isEmpty || !remTag.proposesClassifys.isEmpty && remTag.proposesClassifys.toSet.subsetOf(classifications.toSet))
        }.foreach { cr =>
          cr.status = CONFLICT
          discourse.add(cr)
        }
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

    KarmaUpdate.persistWithProposedTags(karmaDefinition, KarmaQueryUserPost(userDef, postDef), tagsDef)
  }
}

