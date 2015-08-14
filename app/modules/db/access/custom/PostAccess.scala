package modules.db.access.custom

import controllers.api.nodes.RequestContext
import formatters.json.RequestFormat._
import model.WustSchema.{Created => SchemaCreated, _}
import modules.db.Database.db
import modules.db.access.{NodeReadBase, NodeDeleteBase}
import modules.requests._
import play.api.libs.json.JsValue
import renesca.parameter.implicits._
import play.api.mvc.Results._

case class PostAccess() extends NodeReadBase[Post] with NodeDeleteBase[Post] {
  val factory = Post

  private def tagDefGraph(addedTags: List[TagConnectRequest]): Discourse = {
    val discourse = Discourse.empty
    val nodes = addedTags.flatMap { tag =>
      if (tag.id.isDefined)
        Some(TagLike.matchesOnUuid(tag.id.get))
      else if (tag.title.isDefined)
        Some(Categorization.merge(title = tag.title.get, merge = Set("title")))
      else
        None
    }

    discourse.add(nodes: _*)
    discourse
  }

  private def addTagsToGraph(discourse: Discourse, user: User, node: Post) {
    discourse.tagLikes.foreach(tag => {
      val tags = Tags.merge(tag, node)
      //TODO initial votes
      discourse.add(tags)
    })
  }

  //TODO: this is two extra requests...
  private def deleteTagsFromGraph(removedTags: List[String], uuid: String) {
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

  override def create(context: RequestContext) = {
    context.withJson { (request: TaggedPostAddRequest) =>
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
    context.withJson { (request: TaggedPostUpdateRequest) =>
      deleteTagsFromGraph(request.removedTags, uuid)
      val discourse = tagDefGraph(request.addedTags)

      val node = Post.matchesOnUuid(uuid)
      discourse.add(node)
      if(request.description.isDefined) {
        //TODO: normally we would want to set it back to None instead of ""
        // but matches nodes currently cannot save deletions of properties as long
        // as they are local. this is also true for merge nodes!
        // if(request.description.get.isEmpty)
        //   node.description = None
        // else
        node.description = request.description
      }

      if(request.title.isDefined)
        node.title = request.title.get

      if(request.title.isDefined || request.description.isDefined) {
        val contribution = Updated.create(context.user, node)
        discourse.add(contribution)
      }

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
