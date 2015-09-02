package modules.db.access.custom

import controllers.api.nodes.RequestContext
import formatters.json.RequestFormat._
import model.WustSchema.{Created => SchemaCreated, _}
import modules.db.Database.db
import modules.db.access._
import modules.requests._
import play.api.libs.json.JsValue
import renesca.parameter.implicits._
import play.api.mvc.Results._
import model.Helpers.tagTitleColor

trait ConnectableAccessBase {
  private def tagConnectRequestToTag(tag: TagConnectRequest) = {
      if (tag.id.isDefined)
        Some(TagLike.matchesOnUuid(tag.id.get))
      else if (tag.title.isDefined)
        Some(Categorization.merge(
          title = tag.title.get,
          color = tagTitleColor(tag.title.get),
          merge = Set("title")))
      else
        None
  }

  protected def addTagsToGraph(discourse: Discourse, request: AddTagRequestBase, node: Taggable) {
    request.addedTags.flatMap(tagConnectRequestToTag(_)).foreach { tag =>
      val tags = Tags.merge(tag, node)
      //TODO initial votes
      discourse.add(tags)
    }
  }

  protected def addRequestTagsToGraph(discourse: Discourse, user: User, post: Post, request: AddTagRequestBase with RemoveTagRequestBase) {
    //TODO: initialize added tags = karma
    //TODO: initialize removed tags = (current threshold - karma)
    val tagRequests = request.addedTags.flatMap(tagConnectRequestToTag(_)) ++ request.removedTags.map(TagLike.matchesOnUuid(_))
    tagRequests.foreach { tag =>
      val updatedTags = UpdatedTags.create(user, post, applyThreshold = 5, applyVotes = 0)
      discourse.add(Tags.create(tag, updatedTags))
    }
  }

  //TODO: this is two extra requests...
  protected def deleteTagsFromGraph(request: RemoveTagRequestBase, uuid: String) {
    if (request.removedTags.isEmpty)
      return

    val node = Post.matchesOnUuid(uuid)
    val discourse = Discourse(node)

    request.removedTags.foreach { tagUuid =>
      val tag = TagLike.matchesOnUuid(tagUuid)
      discourse.add(tag)
      val tagging = Tags.matches(tag, node)
      discourse.add(tagging)
    }

    //TODO: we need to resolve matches here, otherwise deletion of local nodes does not work
    //TODO: persisting might fail if there is a concurrent request, as matches nodes will fail when they cannot be resolved
    db.persistChanges(discourse)
    discourse.remove(discourse.tags: _*)
    db.persistChanges(discourse)
  }
}

case class PostAccess() extends ConnectableAccessBase with NodeReadBase[Post] with NodeDeleteBase[Post] {
  val factory = Post

  override def create(context: RequestContext) = {
    context.withUser { user =>
      context.withJson { (request: PostAddRequest) =>
        val discourse = Discourse.empty

        val node = Post.create(title = request.title, description = request.description)
        val contribution = SchemaCreated.create(user, node)
        discourse.add(node, contribution)

        addTagsToGraph(discourse, request, node)

        db.transaction(_.persistChanges(discourse)) match {
          case Some(err) => Left(BadRequest(s"Cannot create Post: $err'"))
          case _         => Right(node)
        }
      }
    }
  }

  override def update(context: RequestContext, uuid: String) = {
    context.withUser { user =>
      context.withJson { (request: PostUpdateRequest) =>
        deleteTagsFromGraph(request, uuid)
        val discourse = Discourse.empty

        //TODO: check for edit threshold and implement instant edit
        val node = if (request.title.isDefined || request.description.isDefined) {
          val node = Post.matchesOnUuid(uuid)
          //need to persist node in order to access title/description
          db.persistChanges(node)

          discourse.add(node)

          //TODO: correct threshold and votes for apply
          val contribution = Updated.create(user, node, oldTitle = node.title, newTitle = request.title.getOrElse(node.title), oldDescription = node.description, newDescription = request.description.orElse(node.description), applyThreshold = 5, applyVotes = 0)
          discourse.add(contribution)
          node
        } else {
          Post.matchesOnUuid(uuid)
        }

        addRequestTagsToGraph(discourse, user, node, request)
        discourse.add(node)

        db.transaction(_.persistChanges(discourse)) match {
          case Some(err) => Left(BadRequest(s"Cannot update Post with uuid '$uuid': $err'"))
          //FIXME: why the fuck do i need to do this???
          //otherwise node.rev_tags is empty? something is messed up here.
          case _         => Right(discourse.posts.find(_.uuid == node.uuid).get)
          // case _         => Right(node)
        }
      }
    }
  }
}

case class ConnectableAccess() extends ConnectableAccessBase with NodeReadBase[Connectable] {
  val postAccess = PostAccess()
  val factory = Connectable

  // we redirect the create action the PostAccess. It is not possible to create
  // connectables withou any context and usually you want to handle connects
  // and posts in one api, so usally creating a post is what you want
  override def create(context: RequestContext) = postAccess.create(context)

  override def update(context: RequestContext, uuid: String) = {
    context.withUser { user =>
      context.withJson { (request: ConnectableUpdateRequest) =>
        deleteTagsFromGraph(request, uuid)
        val discourse = Discourse.empty
        val node = Connectable.matchesOnUuid(uuid)
        discourse.add(node)
        addTagsToGraph(discourse, request, node)

        db.transaction(_.persistChanges(discourse)) match {
          case Some(err) => Left(BadRequest(s"Cannot update Post with uuid '$uuid': $err'"))
          case _         => Right(discourse.connectables.find(_.uuid == node.uuid).get)
        }
      }
    }
  }
}
