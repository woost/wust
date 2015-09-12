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
  protected def addRequestTagsToGraph(discourse: Discourse, user: User, post: Post, request: AddTagRequestBase with RemoveTagRequestBase, weight: Long, threshold: Long) {
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
          val addTags = AddTags.create(user, post, applyThreshold = threshold, approvalSum = weight)
          discourse.add(ProposesTag.create(addTags, tag))
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
        val remTags = RemoveTags.create(user, post, applyThreshold = threshold, approvalSum = weight)
        val tag = Scope.matchesOnUuid(tagReq)
        discourse.add(ProposesTag.create(remTags, tag))
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

    //TODO: we need to resolve matches here, otherwise deletion of local nodes does not work
    //TODO: persisting might fail if there is a concurrent request, as matches nodes will fail when they cannot be resolved
    tx.persistChanges(discourse)
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

    //TODO: we need to resolve matches here, otherwise deletion of local nodes does not work
    //TODO: persisting might fail if there is a concurrent request, as matches nodes will fail when they cannot be resolved
    tx.persistChanges(discourse)
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
    val nodeDef = FactoryUuidNodeDefinition(factory, uuid)
    val tagsDef = HyperNodeDefinition(tagDef, Tags, nodeDef)

    val query = s"""
    match ${nodeDef.toQuery} optional match ${tagsDef.toQuery(false, true)}
    return *
    """

    val discourse = Discourse(db.queryGraph(Query(query, tagsDef.parameterMap)))
    discourse.posts.headOption match {
      case Some(node) =>
        context.user.foreach { user =>
          Future {
            //TODO: set viewed on current post when logging in
            db.transaction(_.persistChanges(Viewed.merge(user, node)))
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
        val discourse = Discourse.empty

        val approvalSum = 1 // TODO: karma
        val applyThreshold = 5 // TODO: correct edit threshold
        if (approvalSum >= applyThreshold)
          throw new Exception("Instant edits are not implemented yet!")

        //TODO: check for edit threshold and implement instant edit
        val node = if (request.title.isDefined || request.description.isDefined) {
          val node = Post.matchesOnUuid(uuid)
          //need to persist node in order to access title/description
          tx.persistChanges(node)

          discourse.add(node)

          //TODO: correct threshold and votes for apply
          val contribution = Updated.create(user, node, oldTitle = node.title, newTitle = request.title.getOrElse(node.title), oldDescription = node.description, newDescription = request.description.orElse(node.description), applyThreshold = applyThreshold, approvalSum = approvalSum)
          val votes = Votes.create(user, contribution, weight = approvalSum)
          discourse.add(contribution, votes)
          node
        } else {
          Post.matchesOnUuid(uuid)
        }

        addRequestTagsToGraph(discourse, user, node, request, approvalSum, applyThreshold)
        discourse.add(node)

        tx.persistChanges(discourse) match {
          case Some(err) => Left(BadRequest(s"Cannot update Post with uuid '$uuid': $err'"))
          //FIXME: why the fuck do i need to do this???
          //otherwise node.rev_tags is empty? something is messed up here.
          case _         => Right(tagTaggable.shapeResponse(discourse.posts.find(_.uuid == node.uuid).get))
          // case _         => Right(node)
        }
      }
    }
  }
}

case class ConnectableAccess() extends NodeReadBase[Connectable] {
  val factory = Connectable
  val postAccess = PostAccess()
  val connectsAccess = ConnectsAccess()

  // we redirect the create action the PostAccess. It is not possible to create
  // connectables withou any context and usually you want to handle connects
  // and posts in one api, so usally creating a post is what you want
  override def create(context: RequestContext) = postAccess.create(context)

  // TODO: add parameter to set which access should be used for update
  override def update(context: RequestContext, uuid: String) = connectsAccess.update(context, uuid)
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
