package modules.db.access.custom

import controllers.api.nodes.RequestContext
import formatters.json.RequestFormat._
import play.api.libs.json._
import model.WustSchema.{Created => SchemaCreated, _}
import modules.db.Database._
import modules.db.access._
import modules.db._
import modules.requests._
import renesca.parameter.implicits._
import renesca.QueryHandler
import renesca.Query
import play.api.mvc.Results._
import moderation.Moderation
import scala.concurrent._
import ExecutionContext.Implicits.global

case class PostAccess() extends NodeAccessDefault[Post] with TagAccessHelper {

  val factory = Post

  private def handleInitialChange(contribution: ChangeRequest, user: User, authorBoost: Long, approvalSum: Long) = {
    if (contribution.canApply(authorBoost)) {
      contribution.approvalSum = authorBoost
      contribution.status = APPROVED
      Some(Votes.merge(user, contribution, weight = authorBoost))
    } else if(contribution.canApply(approvalSum)) {
      contribution.status = INSTANT
      None
    } else {
      contribution.approvalSum = approvalSum
      Some(Votes.merge(user, contribution, weight = approvalSum))
    }
  }

  //TODO: refactor
  private def addRequestTagsToGraph(tx: QueryHandler, discourse: Discourse, user: User, post: Post, request: PostUpdateRequest, authorBoost: Long, approvalSum: Long, applyThreshold: Long) {
    // TODO: do not get all connected requests if not needed...its just slow
    // because now we write lock every change request connected to the post
    implicit val ctx = new QueryContext
    val userDef = ConcreteFactoryNodeDefinition(User)
    val requestDef = ConcreteFactoryNodeDefinition(TagChangeRequest)
    val postDef = ConcreteNodeDefinition(post)
    val tagsDef = RelationDefinition(requestDef, ProposesTag, ConcreteFactoryNodeDefinition(Scope))
    val classifiesDef = RelationDefinition(requestDef, ProposesClassify, ConcreteFactoryNodeDefinition(Classification))
    val votesDef = RelationDefinition(ConcreteNodeDefinition(user), Votes, requestDef)

    val query = s"""
    match ${userDef.toQuery}-[ut:`${AddTags.startRelationType}`|`${RemoveTags.startRelationType}`]->(${requestDef.name} ${requestDef.factory.labels.map(l => s":`$l`").mkString} { status: ${PENDING} })-[tp:`${AddTags.endRelationType}`|`${RemoveTags.endRelationType}`]->${postDef.toQuery}, ${tagsDef.toQuery(false,true)}
    optional match ${classifiesDef.toQuery(false, true)}
    optional match ${votesDef.toQuery(true, false)}
    set ${requestDef.name}._locked = true
    return *
    """

    val params = votesDef.parameterMap ++ userDef.parameterMap ++ postDef.parameterMap ++ tagsDef.parameterMap
    val existing = Discourse(tx.queryGraph(query, params))
    discourse.add(existing.nodes: _*)
    discourse.add(existing.relations: _*)
    val existAddTags = existing.addTags
    val existRemTags = existing.removeTags

    request.addedTags.foreach { tagReq =>
      val alreadyExisting = existAddTags.find { addTag =>
        val sameTag = if (tagReq.id.isDefined)
          addTag.proposesTags.head.uuid == tagReq.id.get
        else if (tagReq.title.isDefined)
          addTag.proposesTags.head.title == tagReq.title.get
        else
          false

        sameTag && tagReq.classifications.flatMap(_.id).toSet == addTag.proposesClassifys.map(_.uuid).toSet
      }

      alreadyExisting.foreach { exist =>
        // we only get the vote of the current user, so rev_votes is only
        // defined iff the user already voted on this request
        if (exist.rev_votes.headOption.isEmpty) {
          exist.approvalSum += approvalSum
          discourse.add(Votes.merge(user, exist, weight = approvalSum))
          if (exist.canApply)
            exist.status = APPROVED
        }
      }

      val crOpt = alreadyExisting orElse tagConnectRequestToScope(tagReq).map { tag =>
        // we create a new change request as there is no existing one here
        val addTags = AddTags.create(user, post, applyThreshold = applyThreshold)
        discourse.add(addTags, tag, ProposesTag.create(addTags, tag))
        tagReq.classifications.flatMap(tagConnectRequestToClassification(_)).foreach{classification =>
          discourse.add(ProposesClassify.merge(addTags, classification))
        }
        handleInitialChange(addTags, user, authorBoost = authorBoost, approvalSum = approvalSum).foreach(discourse.add(_))
        addTags
      }

      crOpt.foreach { cr =>
        if (cr.status == INSTANT || cr.status == APPROVED) {
          //disable conlicting change request
          //TODO: code dup
          existAddTags.filter { addTag =>
            val sameTag = if (tagReq.id.isDefined)
              addTag.proposesTags.head.uuid == tagReq.id.get
            else if (tagReq.title.isDefined)
              addTag.proposesTags.head.title == tagReq.title.get
            else
              false

            sameTag && addTag.proposesClassifys.map(_.uuid).toSet.subsetOf(tagReq.classifications.flatMap(_.id).toSet)
          }.foreach(_.status = CONFLICT)

          val tags = Tags.merge(cr.proposesTags.head, post)
          discourse.add(tags)
          cr.proposesClassifys.foreach{classification =>
            discourse.add(Classifies.merge(classification, tags))
          }
        }
      }
    }

    //TODO: deletion of classifications?
    request.removedTags.foreach { tagReq =>
      val alreadyExisting = existRemTags.find { remTag =>
        remTag.proposesTags.head.uuid == tagReq.id && tagReq.classifications.flatMap(_.id).toSet == remTag.proposesClassifys.map(_.uuid).toSet
      }

      alreadyExisting.foreach { exist =>
        if (exist.rev_votes.headOption.isEmpty) {
          exist.approvalSum += approvalSum
          discourse.add(Votes.merge(user, exist, weight = approvalSum))
          if (exist.canApply)
            exist.status = APPROVED
        }
      }

      val cr = alreadyExisting getOrElse {
        val remTags = RemoveTags.create(user, post, applyThreshold = applyThreshold)
        val tag = Scope.matchesOnUuid(tagReq.id)
        discourse.add(remTags, tag, ProposesTag.create(remTags, tag))
        tagReq.classifications.map(c => Classification.matchesOnUuid(c.id)).foreach{ classification =>
          discourse.add(ProposesClassify.merge(remTags, classification))
        }
        handleInitialChange(remTags, user, authorBoost = authorBoost, approvalSum = approvalSum).foreach(discourse.add(_))
        remTags
      }

      if (cr.status == INSTANT || cr.status == APPROVED) {
        val tags = Tags.matches(cr.proposesTags.head, post)
        if (cr.proposesClassifys.isEmpty) {
          discourse.remove(tags)
        } else {
          //disable conlicting change request
          //TODO: code dup
          existRemTags.filter { remTag =>
            remTag.proposesTags.head.uuid == tagReq.id && (!remTag.proposesClassifys.isEmpty || tagReq.classifications.isEmpty) && remTag.proposesClassifys.map(_.uuid).toSet.subsetOf(tagReq.classifications.map(_.id).toSet)
          }.foreach(_.status = CONFLICT)

          discourse.add(tags)
          cr.proposesClassifys.foreach{classification =>
            discourse.remove(Classifies.matches(classification, tags))
          }
        }
      }
    }

    existing.tagChangeRequests.foreach(_._locked = false)
  }

  private def addScopesToGraph(discourse: Discourse, request: PostAddRequest, node: Post) {
    request.addedTags.flatMap(req => tagConnectRequestToScope(req).map((req, _))).foreach { case (req, tag) =>
      val tags = Tags.merge(tag, node)
      discourse.add(tags)
      req.classifications.flatMap(tagConnectRequestToClassification(_)).foreach { classification =>
        discourse.add(Classifies.merge(classification, tags))
      }
    }
  }

  private def viewPost(node: Post, user: User) = Future {
    db.transaction { tx =>
      implicit val ctx = new QueryContext
      val postDef = ConcreteNodeDefinition(node)
      val userDef = ConcreteNodeDefinition(user)
      val viewedDef = RelationDefinition(userDef, Viewed, postDef)
      val query = s"""
      match ${postDef.toQuery}
      set ${postDef.name}._locked = true
      with ${postDef.name}
      optional match ${viewedDef.toQuery(true, false)}
      return *
      """

      val discourse = Discourse(tx.queryGraph(Query(query, viewedDef.parameterMap)))
      val post = discourse.posts.head
      discourse.vieweds.headOption match {
        case Some(viewed) =>
          viewed.timestamp = System.currentTimeMillis
        case None =>
          discourse.add(Viewed.merge(user, post))
          post.viewCount += 1
      }

      post._locked = false
      tx.persistChanges(discourse)
    }
  }

  override def read(context: RequestContext, uuid: String) = {
    import formatters.json.PostFormat._

    implicit val ctx = new QueryContext
    val tagDef = ConcreteFactoryNodeDefinition(Scope)
    val nodeDef = FactoryUuidNodeDefinition(factory, uuid)
    val createdDef = RelationDefinition(ConcreteFactoryNodeDefinition(User), SchemaCreated, nodeDef)
    val connectsDef = ConcreteFactoryNodeDefinition(Connects)
    val tagsDef = HyperNodeDefinition(tagDef, Tags, nodeDef)
    val tagClassDef = ConcreteFactoryNodeDefinition(Classification)
    val tagClassifiesDef = RelationDefinition(tagClassDef, Classifies, tagsDef)
    val connDef = RelationDefinition(nodeDef, ConnectsStart, connectsDef)
    val classDef = ConcreteFactoryNodeDefinition(Classification)
    val classifiesDef = RelationDefinition(classDef, Classifies, connectsDef)

    val (ownVoteCondition, ownVoteParams) = context.user.map { user =>
      val userDef = ConcreteNodeDefinition(user)
      val votesDef = RelationDefinition(userDef, Votes, tagsDef)
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

    val discourse = Discourse(db.queryGraph(Query(query, params)))
    discourse.posts.headOption match {
      case Some(node) =>
        if (context.countView)
          context.user.foreach(viewPost(node, _))

        Ok(Json.toJson(node))
      case None =>
        NotFound(s"Cannot find node with uuid '$uuid'")
    }
  }

  def createNode(context: RequestContext): Either[String,Post] = context.user.map { user =>
    import formatters.json.EditNodeFormat._

    context.jsonAs[PostAddRequest].map { request =>
      val discourse = Discourse.empty

      val node = Post.create(title = request.title, description = request.description)
      val contribution = SchemaCreated.create(user, node)
      discourse.add(node, contribution)

      addScopesToGraph(discourse, request, node)

      db.transaction(_.persistChanges(discourse)) match {
        case Some(err) => Left(err)
        case None      => Right(node)
      }
    }.getOrElse(Left("Cannot parse create request"))
  }.getOrElse(Left("Only for users"))

  override def create(context: RequestContext) = context.withUser {
    import formatters.json.EditNodeFormat._

    createNode(context) match {
      case Left(err) => BadRequest(s"Cannot create Post: $err")
      case Right(node) => Ok(Json.toJson(node))
    }
  }

  override def delete(context: RequestContext, uuid: String) = context.withUser { user =>
    import formatters.json.EditNodeFormat._

    db.transaction { tx =>
      implicit val ctx = new QueryContext
      val postDef = FactoryUuidNodeDefinition(Post, uuid)
      val deletedDef = HyperNodeDefinition(ConcreteFactoryNodeDefinition(User), Deleted, postDef)
      val userDef = ConcreteNodeDefinition(user)
      val createdDef = RelationDefinition(userDef, SchemaCreated, postDef)
      val votesDef = RelationDefinition(userDef, Votes, deletedDef)

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
        val karma = Moderation.voteWeightFromScopes(discourse.scopes)
        val approvalSum = karma + authorBoost

        val deleted = discourse.deleteds.headOption.map { deleted =>
          if (deleted.rev_votes.headOption.isEmpty) {
            deleted.approvalSum += approvalSum
            discourse.add(Votes.merge(user, deleted, weight = approvalSum))
            if (deleted.canApply)
              deleted.status = APPROVED
          }
          deleted
        }.getOrElse{
          val applyThreshold = Moderation.postChangeThreshold(post.viewCount)
          val deleted = Deleted.create(user, post, applyThreshold = applyThreshold)
          discourse.add(deleted)

          handleInitialChange(deleted, user, authorBoost = authorBoost, approvalSum = approvalSum).foreach(discourse.add(_))
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
        val postDef = FactoryUuidNodeDefinition(Post, uuid)
        val userDef = ConcreteNodeDefinition(user)
        val createdDef = RelationDefinition(userDef, SchemaCreated, postDef)

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
          val karma = Moderation.voteWeightFromScopes(discourse.scopes)
          val approvalSum = karma + authorBoost
          val applyThreshold = Moderation.postChangeThreshold(node.viewCount)

          if (request.title.isDefined && request.title.get != node.title || request.description.isDefined && request.description != node.description) {
            val contribution = Updated.create(user, node, oldTitle = node.title, newTitle = request.title.getOrElse(node.title), oldDescription = node.description, newDescription = request.description.orElse(node.description), applyThreshold = applyThreshold)
            discourse.add(contribution)

            handleInitialChange(contribution, user, authorBoost = authorBoost, approvalSum = approvalSum).foreach(discourse.add(_))

            if (contribution.status == INSTANT || contribution.status == APPROVED) {
              request.title.foreach(node.title = _)
              request.description.foreach(d => node.description = if (d.trim.isEmpty) None else Some(d))
            }
          }

          addRequestTagsToGraph(tx, discourse, user, node, request, authorBoost = authorBoost, approvalSum = approvalSum, applyThreshold = applyThreshold)

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
