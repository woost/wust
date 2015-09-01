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
  protected def tagDefGraph(addedTags: List[TagConnectRequest]): Discourse = {
    val discourse = Discourse.empty
    val nodes = addedTags.flatMap { tag =>
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

    discourse.add(nodes: _*)
    discourse
  }

  protected def addTagsToGraph(discourse: Discourse, user: User, node: Connectable) {
    //FIXME: bug in renesca-magic #22
    // discourse.tagLikes.foreach(tag => {
    discourse.nodesAs(TagLike).foreach(tag => {
      val tags = Tags.merge(tag, node)
      //TODO initial votes
      discourse.add(tags)
    })
  }

  //TODO: this is two extra requests...
  protected def deleteTagsFromGraph(removedTags: List[String], uuid: String) {
    if (removedTags.isEmpty)
      return

    val node = Post.matchesOnUuid(uuid)
    val discourse = Discourse(node)

    removedTags.foreach { tagUuid =>
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
    context.withJson { (request: PostAddRequest) =>
      val discourse = tagDefGraph(request.addedTags)

      val node = Post.create(title = request.title, description = request.description)
      val contribution = SchemaCreated.create(context.user, node)
      discourse.add(node, contribution)

      addTagsToGraph(discourse, context.user, node)

      db.transaction(_.persistChanges(discourse)) match {
        case Some(err) => Left(BadRequest(s"Cannot create Post: $err'"))
        case _         => Right(node)
      }
    }
  }

  override def update(context: RequestContext, uuid: String) = {
    context.withJson { (request: PostUpdateRequest) =>
      deleteTagsFromGraph(request.removedTags, uuid)
      val discourse = tagDefGraph(request.addedTags)

      val node = if (request.title.isDefined || request.description.isDefined) {
        //need to persist node in order to access title/description
        val node = Post.matchesOnUuid(uuid)
        db.persistChanges(node)

        discourse.add(node)

        //TODO: correct threshold and votes for apply
        val contribution = Updated.create(context.user, node, oldTitle = node.title, newTitle = request.title.getOrElse(node.title), oldDescription = node.description, newDescription = request.description.orElse(node.description), applyThreshold = 5, applyVotes = 0)
        discourse.add(contribution)
        node
      } else {
        Post.matchesOnUuid(uuid)
      }

      discourse.add(node)
      addTagsToGraph(discourse, context.user, node)

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

case class ConnectableAccess() extends ConnectableAccessBase with NodeReadBase[Connectable] {
  val postAccess = PostAccess()
  val factory = Connectable

  override def create(context: RequestContext) = postAccess.create(context)
  override def update(context: RequestContext, uuid: String) = {
    context.withJson { (request: ConnectableUpdateRequest) =>
      deleteTagsFromGraph(request.removedTags, uuid)
      val discourse = tagDefGraph(request.addedTags)
      val node = Connectable.matchesOnUuid(uuid)
      discourse.add(node)
      addTagsToGraph(discourse, context.user, node)

      db.transaction(_.persistChanges(discourse)) match {
        case Some(err) => Left(BadRequest(s"Cannot update Post with uuid '$uuid': $err'"))
        case _         => Right(discourse.connectables.find(_.uuid == node.uuid).get)
      }
    }
  }
}
