package modules.db.access.custom

import controllers.api.nodes.RequestContext
import controllers.live.LiveWebSocket
import formatters.json.RequestFormat._
import play.api.libs.json._
import model.WustSchema.{Created => SchemaCreated, _}
import modules.db.Database._
import modules.db.access._
import modules.db.helpers.{PostHelper, RequestHelper, TaggedTaggable}
import modules.db._
import modules.requests._
import renesca.parameter.implicits._
import renesca.QueryHandler
import play.api.mvc.Results._
import moderation.Moderation
import common.Constants

case class KarmaProperties(authorBoost: Long, voteWeight: Long, applyThreshold: Long) {
  def approvalSum = authorBoost + voteWeight
}

case class PostAccess() extends NodeAccessDefault[Post] {

  val factory = Post

  private def handleInitialChange(discourse: Discourse, request: ChangeRequest, user: User, karmaProps: KarmaProperties) = {
    if (request.canApply(karmaProps.authorBoost)) {
      request.approvalSum = karmaProps.authorBoost
      request.status = APPROVED
      discourse.add(Votes.merge(user, request, weight = karmaProps.authorBoost))
    } else if(request.canApply(karmaProps.approvalSum)) {
      request.status = INSTANT
    } else {
      request.approvalSum = karmaProps.approvalSum
      discourse.add(Votes.merge(user, request, weight = karmaProps.approvalSum))
    }
  }

  private def handleExistingChange(discourse: Discourse, request: ChangeRequest, user: User, karmaProps: KarmaProperties) = {
    // we only get the vote of the current user, so rev_votes is only
    // defined iff the user already voted on this request
    if (request.rev_votes.headOption.isEmpty) {
      request.approvalSum += karmaProps.approvalSum
      discourse.add(request.relationsAs(ProposesTag): _*)
      discourse.add(request.relationsAs(ProposesClassify): _*)
      discourse.add(request, Votes.merge(user, request, weight = karmaProps.approvalSum))
      if (request.canApply)
        request.status = APPROVED
    }
  }

  private def handleAddTags(discourse: Discourse, existAddTags: Seq[AddTags], user: User, post: Post, request: PostUpdateRequest, karmaProps: KarmaProperties) {
    request.addedTags.foreach { tagReq =>
      val alreadyExisting = existAddTags.find { addTag =>
        val sameTag = if (tagReq.id.isDefined)
          addTag.proposesTags.head.uuid == tagReq.id.get
        else if (tagReq.title.isDefined)
          addTag.proposesTags.head.title == tagReq.title.get
        else
          false

        sameTag && tagReq.classifications.map(_.id).toSet == addTag.proposesClassifys.map(_.uuid).toSet
      }

      alreadyExisting.foreach(handleExistingChange(discourse, _, user, karmaProps))

      val crOpt = alreadyExisting orElse PostHelper.tagConnectRequestToScope(tagReq).map { tag =>
        // we create a new change request as there is no existing one here
        val addTags = AddTags.create(user, post, applyThreshold = karmaProps.applyThreshold)
        discourse.add(addTags, tag, ProposesTag.create(addTags, tag))
        tagReq.classifications.map(c => Classification.matchesOnUuid(c.id)).foreach{classification =>
          discourse.add(ProposesClassify.merge(addTags, classification))
        }
        handleInitialChange(discourse, addTags, user, karmaProps)
        addTags
      }

      crOpt.foreach { cr =>
        if (cr.status == INSTANT || cr.status == APPROVED) {
          RequestHelper.conflictingAddTags(cr, existAddTags).foreach { req =>
            req.status = CONFLICT
            discourse.add(req)
          }

          val tags = Tags.merge(cr.proposesTags.head, post)
          discourse.add(tags)
          cr.proposesClassifys.foreach{classification =>
            discourse.add(Classifies.merge(classification, tags))
          }
        }
      }
    }
  }

  private def handleRemoveTags(discourse: Discourse, existRemTags: Seq[RemoveTags], user: User, post: Post, request: PostUpdateRequest, karmaProps: KarmaProperties) {
    request.removedTags.foreach { tagReq =>
      val alreadyExisting = existRemTags.find { remTag =>
        remTag.proposesTags.head.uuid == tagReq.id && tagReq.classifications.map(_.id).toSet == remTag.proposesClassifys.map(_.uuid).toSet
      }

      alreadyExisting.foreach(handleExistingChange(discourse, _, user, karmaProps))

      val cr = alreadyExisting getOrElse {
        val remTags = RemoveTags.create(user, post, applyThreshold = karmaProps.applyThreshold)
        val tag = Scope.matchesOnUuid(tagReq.id)
        discourse.add(remTags, tag, ProposesTag.create(remTags, tag))
        tagReq.classifications.map(c => Classification.matchesOnUuid(c.id)).foreach{ classification =>
          discourse.add(ProposesClassify.merge(remTags, classification))
        }
        handleInitialChange(discourse, remTags, user, karmaProps)
        remTags
      }

      if (cr.status == INSTANT || cr.status == APPROVED) {
        RequestHelper.conflictingRemoveTags(cr, existRemTags).foreach { req =>
          req.status = CONFLICT
          discourse.add(req)
        }

        val tags = Tags.matches(cr.proposesTags.head, post)
        if (cr.proposesClassifys.isEmpty) {
          discourse.remove(tags)
        } else {
          discourse.add(tags)
          cr.proposesClassifys.foreach{classification =>
            discourse.remove(Classifies.matches(classification, tags))
          }
        }
      }
    }
  }

  private def addRequestTagsToGraph(tx: QueryHandler, discourse: Discourse, user: User, post: Post, request: PostUpdateRequest, karmaProps: KarmaProperties) {
    // TODO: do not get all connected requests if not needed...its just slow
    // because now we write lock every change request connected to the post
    implicit val ctx = new QueryContext
    val userDef = FactoryNodeDef(User)
    val requestDef = FactoryNodeDef(TagChangeRequest)
    val postDef = ConcreteNodeDef(post)
    val tagsDef = RelationDef(requestDef, ProposesTag, FactoryNodeDef(Scope))
    val classifiesDef = RelationDef(requestDef, ProposesClassify, FactoryNodeDef(Classification))
    val votesDef = RelationDef(ConcreteNodeDef(user), Votes, requestDef)

    val query = s"""
    match ${userDef.toPattern}-[ut:`${AddTags.startRelationType}`|`${RemoveTags.startRelationType}`]->(${requestDef.name} ${requestDef.factory.labels.map(l => s":`$l`").mkString} { status: ${PENDING} })-[tp:`${AddTags.endRelationType}`|`${RemoveTags.endRelationType}`]->${postDef.toPattern}, ${tagsDef.toPattern(false,true)}
    optional match ${classifiesDef.toPattern(false, true)}
    optional match ${votesDef.toPattern(true, false)}
    set ${requestDef.name}._locked = true
    return *
    """

    val existing = Discourse(tx.queryGraph(query, ctx.params))

    handleAddTags(discourse, existing.addTags, user, post, request, karmaProps)
    handleRemoveTags(discourse, existing.removeTags, user, post, request, karmaProps)

    existing.tagChangeRequests.foreach(_._locked = false)
  }

  override def read(context: RequestContext, uuid: String) = {
    import formatters.json.PostFormat._

    implicit val ctx = new QueryContext
    val nodeDef = FactoryUuidNodeDef(factory, uuid)
    val createdDef = RelationDef(FactoryNodeDef(User), SchemaCreated, nodeDef)
    val tagsDef = HyperNodeDef(FactoryNodeDef(Scope), Tags, nodeDef)
    val tagClassifiesDef = RelationDef(FactoryNodeDef(Classification), Classifies, tagsDef)
    val connectsDef = FactoryNodeDef(Connects)
    val connDef = RelationDef(nodeDef, ConnectsStart, connectsDef)
    val classifiesDef = RelationDef(FactoryNodeDef(Classification), Classifies, connectsDef)

    val userMatcher = context.user.map { user =>
      val userDef = ConcreteNodeDef(user)
      val viewedDef = RelationDef(userDef, Viewed, nodeDef)
      val votesDef = RelationDef(userDef, Votes, tagsDef)

      s"""
      optional match ${viewedDef.toPattern(true, false)}
      optional match ${votesDef.toPattern(true,false)}
      """
    }.getOrElse("")

    val query = s"""
    match ${nodeDef.toPattern}
    optional match ${tagsDef.toPattern(true, false)}
    optional match ${tagClassifiesDef.toPattern(true, false)}
    optional match ${connDef.toPattern(false, true)}, ${classifiesDef.toPattern(true, false)}
    optional match ${createdDef.toPattern(true, false)}
    $userMatcher
    return *
    """

    val discourse = Discourse(db.queryGraph(query, ctx.params))
    discourse.posts.headOption match {
      case Some(node) =>
        context.user.foreach { user =>
          // The post is viewed as a future, because it has to block for incrementing the viewCount
          // So, we increment the viewcount for the user in the response
          if (node.rev_vieweds.isEmpty)
            node.viewCount += 1

          PostHelper.viewPost(node, user)
        }
        Ok(Json.toJson(node))
      case None =>
        NotFound(s"Cannot find node with uuid '$uuid'")
    }
  }

  override def create(context: RequestContext) = context.withUser { user =>
    import formatters.json.EditNodeFormat._

    context.jsonAs[PostAddRequest].map { request =>

      val discourse = PostHelper.createPost(request, user)
      db.transaction(_.persistChanges(discourse)) match {
        case Some(err) => BadRequest(s"Cannot create Post: $err")
        case None =>
          val post = discourse.posts.head
          LiveWebSocket.sendPostAdd(post)
          Ok(Json.toJson(post))

      }
    }.getOrElse(BadRequest("Cannot parse create request"))
  }

  override def delete(context: RequestContext, uuid: String) = context.withUser { user =>
    import formatters.json.EditNodeFormat._

    db.transaction { tx =>
      implicit val ctx = new QueryContext
      val postDef = FactoryUuidNodeDef(Post, uuid)
      val deletedDef = HyperNodeDef(FactoryNodeDef(User), Deleted, postDef)
      val userDef = ConcreteNodeDef(user)
      val createdDef = RelationDef(userDef, SchemaCreated, postDef)
      val votesDef = RelationDef(userDef, Votes, deletedDef)

      //TODO: sufficient to lock at the end?
      val query = s"""
      match ${postDef.toPattern}-[:`${Connects.startRelationType}`|`${Connects.endRelationType}` *0..${Constants.karmaTagDepth * 2}]->(connectable: `${Connectable.label}`)
      with distinct ${postDef.name}, connectable
      match ${userDef.toPattern}
      optional match (tag: `${Scope.label}`)-[:`${Tags.startRelationType}`]->(:`${Tags.label}`)-[:`${Tags.endRelationType}`]->(connectable: `${Post.label}`)
      with distinct tag, ${postDef.name}, connectable, ${userDef.name}
      optional match (${userDef.name})-[r:`${HasKarma.relationType}`]->(tag)
      optional match ${deletedDef.toPattern(true, false)} where ${deletedDef.name}.status = ${PENDING}
      optional match ${votesDef.toPattern(false, false)}
      optional match ${createdDef.toPattern(false, false)}
      set ${deletedDef.name}._locked = true
      return *
      """

      val discourse = Discourse(tx.queryGraph(query, ctx.params))
      discourse.posts.headOption.map { post =>
        val authorBoost = if (discourse.createds.isEmpty) 0 else Moderation.authorKarmaBoost
        val voteWeight = Moderation.voteWeightFromScopes(discourse.scopes)
        val applyThreshold = Moderation.postChangeThreshold(post.viewCount)
        val karmaProps = KarmaProperties(authorBoost, voteWeight, applyThreshold)

        val deleted = discourse.deleteds.headOption.map { deleted =>
          handleExistingChange(discourse, deleted, user, karmaProps)
          deleted
        }.getOrElse{
          val deleted = Deleted.create(user, post, applyThreshold = karmaProps.applyThreshold)
          discourse.add(deleted)
          handleInitialChange(discourse, deleted, user, karmaProps)
          if (deleted.status == INSTANT || deleted.status == APPROVED) {
            post.hide()
          }
          deleted
        }

        val failure = tx.persistChanges(discourse)
        if (failure.isDefined)
          BadRequest("Cannot delete post")
        else if (deleted.status == PENDING)
          Ok(Json.toJson(post))
        else {
          LiveWebSocket.sendConnectableDelete(post.uuid)
          NoContent
        }
      }.getOrElse(NotFound(s"Cannot find Post with uuid '$uuid'"))
    }
  }

  override def update(context: RequestContext, uuid: String) = context.withUser { user =>
    import formatters.json.EditNodeFormat._

    context.withJson { (request: PostUpdateRequest) =>
      db.transaction { tx =>
        implicit val ctx = new QueryContext
        val postDef = FactoryUuidNodeDef(Post, uuid)
        val userDef = ConcreteNodeDef(user)
        val createdDef = RelationDef(userDef, SchemaCreated, postDef)

        val query = s"""
          match ${postDef.toPattern}-[:`${Connects.startRelationType}`|`${Connects.endRelationType}` *0..${Constants.karmaTagDepth * 2}]->(connectable: `${Connectable.label}`)
          with distinct ${postDef.name}, connectable
          match ${userDef.toPattern}
          optional match ${createdDef.toPattern(false, false)}
          optional match (tag: `${Scope.label}`)-[:`${Tags.startRelationType}`]->(:`${Tags.label}`)-[:`${Tags.endRelationType}`]->(connectable: `${Post.label}`)
          with distinct tag, ${postDef.name}, connectable, ${userDef.name}, ${createdDef.name}
          optional match (${userDef.name})-[r:`${HasKarma.relationType}`]->(tag)
          return *
        """

        val discourse = Discourse(tx.queryGraph(query, ctx.params))

        discourse.posts.headOption.map { node =>
          PostHelper.viewPost(node, user)

          val authorBoost = if (discourse.createds.isEmpty) 0 else Moderation.authorKarmaBoost
          val voteWeight = Moderation.voteWeightFromScopes(discourse.scopes)
          val applyThreshold = Moderation.postChangeThreshold(node.viewCount)
          val karmaProps = KarmaProperties(authorBoost, voteWeight, applyThreshold)

          if (request.title.isDefined && request.title.get != node.title || request.description.isDefined && request.description != node.description) {
            val contribution = Updated.create(user, node, oldTitle = node.title, newTitle = request.title.getOrElse(node.title), oldDescription = node.description, newDescription = request.description.orElse(node.description), applyThreshold = karmaProps.applyThreshold)
            discourse.add(contribution)
            handleInitialChange(discourse, contribution, user, karmaProps)
            if (contribution.status == INSTANT || contribution.status == APPROVED) {
              request.title.foreach(node.title = _)
              request.description.foreach(d => node.description = if (d.trim.isEmpty) None else Some(d))
            }
          }

          addRequestTagsToGraph(tx, discourse, user, node, request, karmaProps)

          tx.persistChanges(discourse) match {
            case Some(err) => BadRequest(s"Cannot update Post with uuid '$uuid': $err")
            case _         =>
              tx.commit() //otherwise taggedtaggable won't see the changes on another transactions
              val post = TaggedTaggable.shapeResponse(node)
              if (discourse.changeRequests.exists(_.status != PENDING))
                LiveWebSocket.sendConnectableUpdate(post)
              Ok(Json.toJson(post))
          }
        } getOrElse {
          BadRequest(s"Cannot find Post with uuid '$uuid'")
        }
      }
    }
  }
}
