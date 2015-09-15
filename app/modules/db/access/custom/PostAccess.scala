package modules.db.access.custom

import controllers.api.nodes.RequestContext
import formatters.json.RequestFormat._
import model.WustSchema.{Created => SchemaCreated, _}
import modules.db.Database._
import modules.db.access._
import modules.db._
import modules.requests._
import play.api.libs.json.JsValue
import renesca.parameter.implicits._
import renesca.QueryHandler
import renesca.Query
import play.api.mvc.Results._
import model.Helpers.tagTitleColor

trait ConnectableAccessBase {
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

  private def tagConnectRequestToClassification(tag: TagConnectRequest) = {
      if (tag.id.isDefined)
        Some(Classification.matchesOnUuid(tag.id.get))
      else if (tag.title.isDefined)
        Some(Classification.merge(
          title = tag.title.get,
          color = tagTitleColor(tag.title.get),
          merge = Set("title")))
      else
        None
  }

  protected def addScopesToGraph(discourse: Discourse, request: AddTagRequestBase, node: Post) {
    request.addedTags.flatMap(tagConnectRequestToScope(_)).foreach { tag =>
      val tags = Tags.merge(tag, node)
      discourse.add(tags)
    }
  }

  protected def addClassifcationsToGraph(discourse: Discourse, request: AddTagRequestBase, node: Connects) {
    request.addedTags.flatMap(tagConnectRequestToClassification(_)).foreach { tag =>
      val tags = Classifies.merge(tag, node)
      discourse.add(tags)
    }
  }

  //TODO: refactor
  protected def addRequestTagsToGraph(tx: QueryHandler, discourse: Discourse, user: User, post: Post, request: AddTagRequestBase with RemoveTagRequestBase, weight: Long, threshold: Long, instantApply: Boolean) {
    // TODO: checking for duplicates is not really safe if there are concurrent requests
    // TODO: do not get all connected requests if not needed...its just slow
    val userDef = ConcreteFactoryNodeDefinition(User)
    val tagDef = ConcreteFactoryNodeDefinition(Scope)
    val requestDef = ConcreteFactoryNodeDefinition(TagChangeRequest)
    val postDef = ConcreteNodeDefinition(post)
    val tagsDef = RelationDefinition(requestDef, ProposesTag, tagDef)
    val query = s"match ${userDef.toQuery}-[ut:`${UserToAddTags.relationType}`|`${UserToRemoveTags.relationType}`]->${requestDef.toQuery}-[tp:`${AddTagsToPost.relationType}`|`${RemoveTagsToPost.relationType}`]->${postDef.toQuery}, ${tagsDef.toQuery(false,true)} return *"
    val existing = Discourse(db.queryGraph(Query(query, userDef.parameterMap ++ postDef.parameterMap ++ tagsDef.parameterMap)))

    discourse.add(existing.tagLikes: _*)
    discourse.add(existing.tags: _*)
    val existAddTags = existing.addTags
    val existRemTags = existing.removeTags

    request.addedTags.foreach { tagReq =>
      //TODO: should vote for alreadyexisting requests
      //needs to lock for caching / share logic with VotesAccess?
      //this is very important for already existing requests when request has
      //instantApply, so we apply the existing change request.
      val alreadyExisting = existAddTags.exists { addTag =>
        if (tagReq.id.isDefined)
          addTag.proposesTags.head.uuid == tagReq.id.get
        else if (tagReq.title.isDefined)
          addTag.proposesTags.head.title == tagReq.title.get
        else
          false
      }

      if (!alreadyExisting) {
        tagConnectRequestToScope(tagReq).map { tag =>
          val addTags = AddTags.create(user, post, applyThreshold = threshold, approvalSum = weight, applied = instantApply)
          discourse.add(ProposesTag.create(addTags, tag))
          if (instantApply) {
            discourse.add(Tags.merge(tag, post))
          }

          addTags
        } foreach { addTags =>
          val votes = Votes.create(user, addTags, weight = weight)
          discourse.add(addTags, votes)
        }
      }
    }

    request.removedTags.foreach { tagReq =>
      val alreadyExisting = existRemTags.exists(_.proposesTags.head.uuid == tagReq)

      if (!alreadyExisting) {
        val remTags = RemoveTags.create(user, post, applyThreshold = threshold, approvalSum = weight, applied = instantApply)
        val tag = Scope.matchesOnUuid(tagReq)
        discourse.add(ProposesTag.create(remTags, tag))
        if (instantApply)
          discourse.remove(Tags.matches(tag, post))

        val votes = Votes.create(user, remTags, weight = weight)
        discourse.add(remTags, votes)
      }
    }
  }

  //TODO: this is two extra requests...
  protected def deleteScopesFromGraph(tx:QueryHandler, request: RemoveTagRequestBase, uuid: String) {
    if (request.removedTags.isEmpty)
      return

    val node = Post.matchesOnUuid(uuid)
    val discourse = Discourse(node)

    request.removedTags.foreach { tagUuid =>
      val tag = Scope.matchesOnUuid(tagUuid)
      discourse.add(tag)
      val tagging = Tags.matches(tag, node)
      discourse.add(tagging)
    }

    discourse.remove(discourse.tags: _*)
    tx.persistChanges(discourse)
  }

//TODO: this is two extra requests...
  protected def deleteClassificationsFromGraph(tx:QueryHandler, request: RemoveTagRequestBase, uuid: String) {
    if (request.removedTags.isEmpty)
      return

    val node = Connects.matchesOnUuid(uuid)
    val discourse = Discourse(node)

    request.removedTags.foreach { tagUuid =>
      val tag = Classification.matchesOnUuid(tagUuid)
      discourse.add(tag)
      val tagging = Classifies.matches(tag, node)
      discourse.add(tagging)
    }

    discourse.remove(discourse.classifies: _*)
    tx.persistChanges(discourse)
  }
}

case class PostAccess() extends ConnectableAccessBase with NodeDeleteBase[Post] {
  import scala.concurrent._
  import ExecutionContext.Implicits.global

  val factory = Post
  //TODO: no tagtaggable, query directly
  val tagTaggable = TaggedTaggable.apply[Post]

  override def read(context: RequestContext) = {
    Right(tagTaggable.shapeResponse(context.page.map { page =>
      val skip = page * context.limit
      limitedDiscourseNodes(skip, context.limit, factory)._2
    }.getOrElse(discourseNodes(factory)._2)))
  }

  override def read(context: RequestContext, uuid: String) = {
    val tagDef = ConcreteFactoryNodeDefinition(Scope)
    val classDef = ConcreteFactoryNodeDefinition(Classification)
    val nodeDef = FactoryUuidNodeDefinition(factory, uuid)
    val connectsDef = ConcreteFactoryNodeDefinition(Connects)
    val tagsDef = HyperNodeDefinition(tagDef, Tags, nodeDef)
    val connDef = RelationDefinition(nodeDef, PostToConnects, connectsDef)
    val classifiesDef = RelationDefinition(classDef, Classifies, connectsDef)

    val query = s"""
    match ${nodeDef.toQuery}
    optional match ${tagsDef.toQuery(true, false)}
    optional match ${connDef.toQuery(false, true)}, ${classifiesDef.toQuery(true, false)}
    return *
    """

    val params = tagsDef.parameterMap ++ connDef.parameterMap ++ classifiesDef.parameterMap

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

        Right(node)
      case None => Left(NotFound(s"Cannot find node with uuid '$uuid'"))
    }
  }

  override def create(context: RequestContext) = context.withUser { user =>
    context.withJson { (request: PostAddRequest) =>
      val discourse = Discourse.empty

      val node = Post.create(title = request.title, description = request.description)
      val contribution = SchemaCreated.create(user, node)
      discourse.add(node, contribution)

      addScopesToGraph(discourse, request, node)

      db.transaction(_.persistChanges(discourse)) match {
        case Some(err) => Left(BadRequest(s"Cannot create Post: $err'"))
        case _         => Right(node)
      }
    }
  }

  override def update(context: RequestContext, uuid: String) = context.withUser { user =>
    context.withJson { (request: PostUpdateRequest) =>
      db.transaction { tx =>
        val postDef = FactoryUuidNodeDefinition(Post, uuid)
        val userDef = ConcreteNodeDefinition(user)
        val createdDef = RelationDefinition(userDef, SchemaCreated, postDef)

        val query = s"""
          match ${postDef.toQuery}
          optional match ${createdDef.toQuery(true, false)}
          return *
        """

        val discourse = Discourse(tx.queryGraph(Query(query, createdDef.parameterMap)))
        discourse.posts.headOption.map { node =>
          val isAuthor = discourse.createds.headOption.isDefined
          val authorBoost = if (isAuthor) 5 else 0

          val karma = 1 // TODO: karma
          val approvalSum = karma + authorBoost
          val applyThreshold = 5 // TODO: correct edit threshold
          val instantApply = approvalSum >= applyThreshold

          //TODO: check for edit threshold and implement instant edit
          if (request.title.isDefined || request.description.isDefined) {
            val contribution = Updated.create(user, node, oldTitle = node.title, newTitle = request.title.getOrElse(node.title), oldDescription = node.description, newDescription = request.description.orElse(node.description), applyThreshold = applyThreshold, approvalSum = approvalSum, applied = instantApply)
            val votes = Votes.create(user, contribution, weight = approvalSum)
            discourse.add(contribution, votes)
            if (instantApply) {
              request.title.foreach(node.title = _)
              request.description.foreach(d => node.description = Some(d))
            }
          }

          addRequestTagsToGraph(tx, discourse, user, node, request, approvalSum, applyThreshold, instantApply)

          tx.persistChanges(discourse) match {
            case Some(err) => Left(BadRequest(s"Cannot update Post with uuid '$uuid': $err"))
            //FIXME: why the fuck do i need to do this???
            //otherwise node.rev_tags is empty? something is messed up here.
            case _         => Right(discourse.posts.find(_.uuid == node.uuid).get)
            // case _         => Right(node)
          }
        } getOrElse {
          Left(BadRequest(s"Cannot find Post with uuid '$uuid'"))
        }
      }
    }
  }
}

case class ConnectsAccess() extends ConnectableAccessBase with NodeReadBase[Connects] {
  val factory = Connects
  val classifiedConnects = ClassifiedConnects.apply[Connects]

  // updates only work
  override def update(context: RequestContext, uuid: String) = context.withUser { user =>
    context.withJson { (request: ConnectableUpdateRequest) =>
      db.transaction { tx =>
        deleteClassificationsFromGraph(tx, request, uuid)

        val discourse = Discourse.empty
        val node = Connects.matchesOnUuid(uuid)
        discourse.add(node)
        addClassifcationsToGraph(discourse, request, node)

        tx.persistChanges(discourse) match {
          case Some(err) => Left(BadRequest(s"Cannot update Connects with uuid '$uuid': $err'"))
          case _         => Right(classifiedConnects.shapeResponse(discourse.connects.find(_.uuid == node.uuid).get))
        }
      }
    }
  }
}
