package modules.db.access.custom

import controllers.api.nodes.RequestContext
import formatters.json.RequestFormat._
import play.api.libs.json._
import model.WustSchema.{Created => SchemaCreated, _}
import modules.db.Database._
import modules.db.access._
import modules.db.helpers.{PostHelper, RequestHelper}
import modules.db._
import modules.requests._
import renesca.parameter.implicits._
import renesca.QueryHandler
import play.api.mvc.Results._
import moderation.Moderation

case class KarmaProperties(authorBoost: Long, voteWeight: Long, applyThreshold: Long) {
  def approvalSum = authorBoost + voteWeight
}

case class PostAccess() extends NodeAccessDefault[Post] {

  val factory = Post

  private def handleInitialChange(contribution: ChangeRequest, user: User, karmaProps: KarmaProperties) = {
    if (contribution.canApply(karmaProps.authorBoost)) {
      contribution.approvalSum = karmaProps.authorBoost
      contribution.status = APPROVED
      Some(Votes.merge(user, contribution, weight = karmaProps.authorBoost))
    } else if(contribution.canApply(karmaProps.approvalSum)) {
      contribution.status = INSTANT
      None
    } else {
      contribution.approvalSum = karmaProps.approvalSum
      Some(Votes.merge(user, contribution, weight = karmaProps.approvalSum))
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

      alreadyExisting.foreach { exist =>
        // we only get the vote of the current user, so rev_votes is only
        // defined iff the user already voted on this request
        if (exist.rev_votes.headOption.isEmpty) {
          exist.approvalSum += karmaProps.approvalSum
          discourse.add(Votes.merge(user, exist, weight = karmaProps.approvalSum))
          if (exist.canApply)
            exist.status = APPROVED
        }
      }

      val crOpt = alreadyExisting orElse PostHelper.tagConnectRequestToScope(tagReq).map { tag =>
        // we create a new change request as there is no existing one here
        val addTags = AddTags.create(user, post, applyThreshold = karmaProps.applyThreshold)
        discourse.add(addTags, tag, ProposesTag.create(addTags, tag))
        tagReq.classifications.map(c => Classification.matchesOnUuid(c.id)).foreach{classification =>
          discourse.add(ProposesClassify.merge(addTags, classification))
        }
        handleInitialChange(addTags, user, karmaProps).foreach(discourse.add(_))
        addTags
      }

      crOpt.foreach { cr =>
        if (cr.status == INSTANT || cr.status == APPROVED) {
          RequestHelper.conflictingAddTags(cr, existAddTags).foreach(_.status = CONFLICT)

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

      alreadyExisting.foreach { exist =>
        if (exist.rev_votes.headOption.isEmpty) {
          exist.approvalSum += karmaProps.approvalSum
          discourse.add(Votes.merge(user, exist, weight = karmaProps.approvalSum))
          if (exist.canApply)
            exist.status = APPROVED
        }
      }

      val cr = alreadyExisting getOrElse {
        val remTags = RemoveTags.create(user, post, applyThreshold = karmaProps.applyThreshold)
        val tag = Scope.matchesOnUuid(tagReq.id)
        discourse.add(remTags, tag, ProposesTag.create(remTags, tag))
        tagReq.classifications.map(c => Classification.matchesOnUuid(c.id)).foreach{ classification =>
          discourse.add(ProposesClassify.merge(remTags, classification))
        }
        handleInitialChange(remTags, user, karmaProps).foreach(discourse.add(_))
        remTags
      }

      if (cr.status == INSTANT || cr.status == APPROVED) {
        RequestHelper.conflictingRemoveTags(cr, existRemTags).foreach(_.status = CONFLICT)

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
    match ${userDef.toQuery}-[ut:`${AddTags.startRelationType}`|`${RemoveTags.startRelationType}`]->(${requestDef.name} ${requestDef.factory.labels.map(l => s":`$l`").mkString} { status: ${PENDING} })-[tp:`${AddTags.endRelationType}`|`${RemoveTags.endRelationType}`]->${postDef.toQuery}, ${tagsDef.toQuery(false,true)}
    optional match ${classifiesDef.toQuery(false, true)}
    optional match ${votesDef.toQuery(true, false)}
    set ${requestDef.name}._locked = true
    return *
    """

    val params = votesDef.parameterMap ++ userDef.parameterMap ++ postDef.parameterMap ++ tagsDef.parameterMap
    val existing = Discourse(tx.queryGraph(query, params))
    //TODO should only add conflicting change requests + the one we are voting for including its tags
    discourse.add(existing.nodes: _*)
    discourse.add(existing.relations: _*)

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

    val (ownVoteCondition, ownVoteParams) = context.user.map { user =>
      val userDef = ConcreteNodeDef(user)
      val votesDef = RelationDef(userDef, Votes, tagsDef)
      (s"optional match ${votesDef.toQuery(true,false)}", votesDef.parameterMap)
    }.getOrElse(("", Map.empty))

    val query = s"""
    match ${nodeDef.toQuery}
    optional match ${tagsDef.toQuery(true, false)}
    optional match ${tagClassifiesDef.toQuery(true, false)}
    $ownVoteCondition
    optional match ${connDef.toQuery(false, true)}, ${classifiesDef.toQuery(true, false)}
    optional match ${createdDef.toQuery(true, false)}
    return *
    """

    val params = tagsDef.parameterMap ++ connDef.parameterMap ++ tagClassifiesDef.parameterMap ++ createdDef.parameterMap ++ classifiesDef.parameterMap ++ ownVoteParams

    val discourse = Discourse(db.queryGraph(query, params))
    discourse.posts.headOption match {
      case Some(node) =>
        if (context.countView)
          context.user.foreach(PostHelper.viewPost(node, _))

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
        case None => Ok(Json.toJson(discourse.posts.head))
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
      match ${postDef.toQuery}-[:`${Connects.startRelationType}`|`${Connects.endRelationType}` *0..20]->(connectable: `${Connectable.label}`)
      with distinct ${postDef.name}, connectable
      match ${userDef.toQuery}
      optional match (tag: `${Scope.label}`)-[:`${Tags.startRelationType}`]->(:`${Tags.label}`)-[:`${Tags.endRelationType}`]->(connectable: `${Post.label}`)
      optional match (${userDef.name})-[r:`${HasKarma.relationType}`]->(tag)
      optional match ${deletedDef.toQuery(true, false)} where ${deletedDef.name}.status = ${PENDING}
      optional match ${votesDef.toQuery(false, false)}
      optional match ${createdDef.toQuery(false, false)}
      set ${deletedDef.name}._locked = true
      return *
      """

      val params = votesDef.parameterMap ++ createdDef.parameterMap ++ deletedDef.parameterMap
      val discourse = Discourse(tx.queryGraph(query, params))
      discourse.posts.headOption.map { post =>
        val authorBoost = if (discourse.createds.isEmpty) 0 else Moderation.authorKarmaBoost
        val voteWeight = Moderation.voteWeightFromScopes(discourse.scopes)
        val applyThreshold = Moderation.postChangeThreshold(post.viewCount)
        val karmaProps = KarmaProperties(authorBoost, voteWeight, applyThreshold)

        val deleted = discourse.deleteds.headOption.map { deleted =>
          if (deleted.rev_votes.headOption.isEmpty) {
            deleted.approvalSum += karmaProps.approvalSum
            discourse.add(Votes.merge(user, deleted, weight = karmaProps.approvalSum))
            if (deleted.canApply)
              deleted.status = APPROVED
          }
          deleted
        }.getOrElse{
          val deleted = Deleted.create(user, post, applyThreshold = karmaProps.applyThreshold)
          discourse.add(deleted)
          handleInitialChange(deleted, user, karmaProps).foreach(discourse.add(_))
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
        else
          NoContent
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
          match ${postDef.toQuery}-[:`${Connects.startRelationType}`|`${Connects.endRelationType}` *0..20]->(connectable: `${Connectable.label}`)
          with distinct ${postDef.name}, connectable
          match ${userDef.toQuery}
          optional match ${createdDef.toQuery(false, false)}
          optional match (tag: `${Scope.label}`)-[:`${Tags.startRelationType}`]->(:`${Tags.label}`)-[:`${Tags.endRelationType}`]->(connectable: `${Post.label}`)
          optional match (${userDef.name})-[r:`${HasKarma.relationType}`]->(tag)
          return *
        """

        val params = createdDef.parameterMap
        val discourse = Discourse(tx.queryGraph(query, params))

        discourse.posts.headOption.map { node =>
          val authorBoost = if (discourse.createds.isEmpty) 0 else Moderation.authorKarmaBoost
          val voteWeight = Moderation.voteWeightFromScopes(discourse.scopes)
          val applyThreshold = Moderation.postChangeThreshold(node.viewCount)
          val karmaProps = KarmaProperties(authorBoost, voteWeight, applyThreshold)

          if (request.title.isDefined && request.title.get != node.title || request.description.isDefined && request.description != node.description) {
            val contribution = Updated.create(user, node, oldTitle = node.title, newTitle = request.title.getOrElse(node.title), oldDescription = node.description, newDescription = request.description.orElse(node.description), applyThreshold = karmaProps.applyThreshold)
            discourse.add(contribution)
            handleInitialChange(contribution, user, karmaProps).foreach(discourse.add(_))
            if (contribution.status == INSTANT || contribution.status == APPROVED) {
              request.title.foreach(node.title = _)
              request.description.foreach(d => node.description = if (d.trim.isEmpty) None else Some(d))
            }
          }

          addRequestTagsToGraph(tx, discourse, user, node, request, karmaProps)

          tx.persistChanges(discourse) match {
            case Some(err) => BadRequest(s"Cannot update Post with uuid '$uuid': $err")
            case _         => Ok(Json.toJson(node))
          }
        } getOrElse {
          BadRequest(s"Cannot find Post with uuid '$uuid'")
        }
      }
    }
  }
}
