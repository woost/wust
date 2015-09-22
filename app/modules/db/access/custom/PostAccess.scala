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
import model.Helpers.tagTitleColor
import moderation.Moderation

case class PostAccess() extends NodeAccessDefault[Post] with TagAccessHelper {

  val factory = Post

  private def handleInitialChange(contribution: ChangeRequest, user: User, authorBoost: Long, approvalSum: Long) = {
    if (contribution.canApply(authorBoost)) {
      contribution.approvalSum = authorBoost
      contribution.applied = APPLIED
      Some(Votes.merge(user, contribution, weight = authorBoost))
    } else if(contribution.canApply(approvalSum)) {
      contribution.approvalSum = approvalSum
      contribution.applied = INSTANT
      None
    } else {
      contribution.approvalSum = approvalSum
      Some(Votes.merge(user, contribution, weight = approvalSum))
    }
  }

  //TODO: refactor
  private def addRequestTagsToGraph(tx: QueryHandler, discourse: Discourse, user: User, post: Post, request: AddTagRequestBase with RemoveTagRequestBase, authorBoost: Long, approvalSum: Long, applyThreshold: Long) {
    // TODO: do not get all connected requests if not needed...its just slow
    // because now we write lock every change request connected to the post
    val userDef = ConcreteFactoryNodeDefinition(User)
    val tagDef = ConcreteFactoryNodeDefinition(Scope)
    val requestDef = ConcreteFactoryNodeDefinition(TagChangeRequest)
    val postDef = ConcreteNodeDefinition(post)
    val tagsDef = RelationDefinition(requestDef, ProposesTag, tagDef)
    val votesDef = RelationDefinition(ConcreteNodeDefinition(user), Votes, requestDef)
    val query = s"match ${userDef.toQuery}-[ut:`${UserToAddTags.relationType}`|`${UserToRemoveTags.relationType}`]->(${requestDef.name} ${requestDef.factory.labels.map(l => s":`$l`").mkString} { applied: ${PENDING} })-[tp:`${AddTagsToPost.relationType}`|`${RemoveTagsToPost.relationType}`]->${postDef.toQuery}, ${tagsDef.toQuery(false,true)} optional match ${votesDef.toQuery(true, false)} set ${requestDef.name}._locked = true return *"
    val existing = Discourse(tx.queryGraph(Query(query, votesDef.parameterMap ++ userDef.parameterMap ++ postDef.parameterMap ++ tagsDef.parameterMap)))

    discourse.add(existing.nodes: _*)
    discourse.add(existing.relations: _*)
    val existAddTags = existing.addTags
    val existRemTags = existing.removeTags

    request.addedTags.foreach { tagReq =>
      val alreadyExisting = existAddTags.find { addTag =>
        if (tagReq.id.isDefined)
          addTag.proposesTags.head.uuid == tagReq.id.get
        else if (tagReq.title.isDefined)
          addTag.proposesTags.head.title == tagReq.title.get
        else
          false
      }

      alreadyExisting.foreach { exist =>
        // we only get the vote of the current user, so rev_votes is only
        // defined iff the user already voted on this request
        if (exist.rev_votes.headOption.isEmpty) {
          exist.approvalSum += approvalSum
          discourse.add(Votes.merge(user, exist, weight = approvalSum))
          if (exist.canApply)
            exist.applied = APPLIED
        }
      }

      val crOpt = alreadyExisting orElse tagConnectRequestToScope(tagReq).map { tag =>
        // we create a new change request as there is no existing one here
        val addTags = AddTags.create(user, post, applyThreshold = applyThreshold, approvalSum = approvalSum)
        discourse.add(addTags, tag, ProposesTag.create(addTags, tag))
        handleInitialChange(addTags, user, authorBoost = authorBoost, approvalSum = approvalSum).foreach(discourse.add(_))
        addTags
      }

      crOpt.foreach { cr =>
        if (cr.applied == INSTANT || cr.applied == APPLIED) {
          discourse.add(Tags.merge(cr.proposesTags.head, post))
        }
      }
    }

    request.removedTags.foreach { tagReq =>
      val alreadyExisting = existRemTags.find(_.proposesTags.head.uuid == tagReq)

      alreadyExisting.foreach { exist =>
        if (exist.rev_votes.headOption.isEmpty) {
          exist.approvalSum += approvalSum
          discourse.add(Votes.merge(user, exist, weight = approvalSum))
          if (exist.canApply)
            exist.applied = APPLIED
        }
      }

      val cr = alreadyExisting getOrElse {
        val remTags = RemoveTags.create(user, post, applyThreshold = applyThreshold, approvalSum = approvalSum)
        val tag = Scope.matchesOnUuid(tagReq)
        discourse.add(remTags, tag, ProposesTag.create(remTags, tag))
        handleInitialChange(remTags, user, authorBoost = authorBoost, approvalSum = approvalSum).foreach(discourse.add(_))
        remTags
      }

      if (cr.applied == INSTANT || cr.applied == APPLIED) {
        discourse.remove(Tags.matches(cr.proposesTags.head, post))
      }
    }

    existing.changeRequests.foreach(_._locked = false)
  }

  private def tagConnectRequestToScope(tag: TagConnectRequest) = {
      if (tag.id.isDefined)
        Some(Scope.matchesOnUuid(tag.id.get))
      else if (tag.title.isDefined)
        Some(Scope.merge(
          title = tag.title.get,
          color = tagTitleColor(tag.title.get),
          merge = Set("title")))
      else
        None
  }

  private def addScopesToGraph(discourse: Discourse, request: AddTagRequestBase, node: Post) {
    request.addedTags.flatMap(req => tagConnectRequestToScope(req).map((req, _))).foreach { case (req, tag) =>
      val tags = Tags.merge(tag, node)
      discourse.add(tags)
      req.classifications.flatMap(tagConnectRequestToClassification(_)).foreach { classification =>
        discourse.add(Classifies.merge(classification, tags))
      }
    }
  }

  private def deleteScopesFromGraph(discourse: Discourse, request: RemoveTagRequestBase, node: Post) {
    request.removedTags.foreach { tagUuid =>
      val tag = Scope.matchesOnUuid(tagUuid)
      discourse.add(tag)
      val tagging = Tags.matches(tag, node)
      discourse.remove(tagging)
    }
  }

  override def read(context: RequestContext, uuid: String) = {
    import scala.concurrent._
    import ExecutionContext.Implicits.global
    import formatters.json.PostFormat._

    val tagDef = ConcreteFactoryNodeDefinition(Scope)
    val nodeDef = FactoryUuidNodeDefinition(factory, uuid)
    val connectsDef = ConcreteFactoryNodeDefinition(Connects)
    val tagsDef = HyperNodeDefinition(tagDef, Tags, nodeDef)
    val tagClassDef = ConcreteFactoryNodeDefinition(Classification)
    val tagClassifiesDef = RelationDefinition(tagClassDef, Classifies, tagsDef)
    val connDef = RelationDefinition(nodeDef, PostToConnects, connectsDef)
    val classDef = ConcreteFactoryNodeDefinition(Classification)
    val classifiesDef = RelationDefinition(classDef, Classifies, connectsDef)

    val (ownVoteCondition, ownVoteParams) = context.user.map { user =>
      val userDef = ConcreteNodeDefinition(user)
      val votesDef = RelationDefinition(userDef, Votes, tagsDef)
      (s"optional match ${votesDef.toQuery(true,false)}", votesDef.parameterMap)
    }.getOrElse(("", Map.empty))

    val query = s"""
    match ${nodeDef.toQuery}
    optional match ${tagsDef.toQuery(true, false)}, ${tagClassifiesDef.toQuery(true, false)}
    $ownVoteCondition
    optional match ${connDef.toQuery(false, true)}, ${classifiesDef.toQuery(true, false)}
    return *
    """

    val params = tagsDef.parameterMap ++ connDef.parameterMap ++ tagClassifiesDef.parameterMap ++ classifiesDef.parameterMap ++ ownVoteParams

    val discourse = Discourse(db.queryGraph(Query(query, params)))
    discourse.posts.headOption match {
      case Some(node) =>
      if (context.countView) {
          context.user.foreach { user =>
            Future {
              db.transaction { tx =>
                val postDef = ConcreteNodeDefinition(node)
                val userDef = ConcreteNodeDefinition(user)
                val viewedDef = RelationDefinition(userDef, Viewed, postDef)
                val query = s"match ${postDef.toQuery} set ${postDef.name}._locked = true with ${postDef.name} optional match ${viewedDef.toQuery(true, false)} return *"
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
          }
        }

        Ok(Json.toJson(node))
      case None => NotFound(s"Cannot find node with uuid '$uuid'")
    }
  }

  def createNode(context: RequestContext): Option[Post] = context.user.flatMap { user =>
    import formatters.json.EditNodeFormat._

    context.jsonAs[PostAddRequest].flatMap { request =>
      val discourse = Discourse.empty

      val node = Post.create(title = request.title, description = request.description)
      val contribution = SchemaCreated.create(user, node)
      discourse.add(node, contribution)

      addScopesToGraph(discourse, request, node)

      db.transaction(_.persistChanges(discourse)) match {
        case Some(err) => None
        case _         => Some(node)
      }
    }
  }

  override def create(context: RequestContext) = context.withUser {
    import formatters.json.EditNodeFormat._

    createNode(context).map(n => Ok(Json.toJson(n))).getOrElse(BadRequest("Cannot create Post"))
  }

  override def delete(context: RequestContext, uuid: String) = context.withUser { user =>
    db.transaction { tx =>
      val node = Post.matchesOnUuid(uuid)
      val failure = tx.persistChanges(node)
      if (failure.isDefined)
        NotFound(s"Cannot find Post with uuid '$uuid'")
      else {
        // TODO: non-instant changes
        val applyThreshold = Moderation.postChangeThreshold(node.viewCount)
        val hidden = node.hide()
        val deleted = Deleted.create(user, hidden, applyThreshold = applyThreshold)
        deleted.applied = INSTANT
        val failure = tx.persistChanges(hidden, deleted)
        if (failure.isDefined)
          BadRequest("Cannot delete node")
        else
          NoContent
      }
    }
  }

  override def update(context: RequestContext, uuid: String) = context.withUser { user =>
    import formatters.json.EditNodeFormat._

    context.withJson { (request: PostUpdateRequest) =>
      db.transaction { tx =>
        val postDef = FactoryUuidNodeDefinition(Post, uuid)
        val userDef = ConcreteNodeDefinition(user)
        val createdDef = RelationDefinition(userDef, SchemaCreated, postDef)

        val query = s"""
          match ${userDef.toQuery},
          ${postDef.toQuery}-[:`${Connects.startRelationType}`|`${Connects.endRelationType}` *0..20]->(connectable: `${Connectable.label}`)
          with distinct connectable, ${userDef.name}
          match (tag: `${Scope.label}`)-[:`${Tags.startRelationType}`]->(:`${Tags.label}`)-[:`${Tags.endRelationType}`]->(connectable: `${Post.label}`)
          optional match (${userDef.name})-[r:`${HasKarma.relationType}`]->(tag)
          optional match ${createdDef.toQuery(true, false)}
          return *
        """

        val params = createdDef.parameterMap
        val discourse = Discourse(tx.queryGraph(query, params))

        discourse.posts.headOption.map { node =>
          val authorBoost = if (discourse.createds.isEmpty) 0 else Moderation.authorKarmaBoost
          val karma = if (discourse.scopes.isEmpty) {
            1
          } else {
            val ratio = discourse.scopes.map { tag =>
              val l:Long = tag.inRelationsAs(HasKarma).headOption.map(_.karma).getOrElse(0)
              l
            }.sum / discourse.scopes.size
            ratio max 1
          }

          val approvalSum = karma + authorBoost
          val applyThreshold = Moderation.postChangeThreshold(node.viewCount)

          if (request.title.isDefined || request.description.isDefined) {
            val contribution = Updated.create(user, node, oldTitle = node.title, newTitle = request.title.getOrElse(node.title), oldDescription = node.description, newDescription = request.description.orElse(node.description), applyThreshold = applyThreshold)
            discourse.add(contribution)

            handleInitialChange(contribution, user, authorBoost = authorBoost, approvalSum = approvalSum).foreach(discourse.add(_))

            if (contribution.canApply) {
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
