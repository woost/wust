package modules.db.access.custom

import controllers.api.nodes.RequestContext
import formatters.json.RequestFormat._
import model.WustSchema._
import modules.db.Database.db
import modules.db.access.{NodeRead, NodeReadDelete}
import modules.requests._
import play.api.libs.json.JsValue
import renesca.parameter.implicits._

class PostAccess extends NodeReadDelete(Post) {
  private def tagDefGraph(addedTags: List[String]): Discourse = {
    val discourse = Discourse.empty
    val nodes = addedTags.map(tag => Tag.matches(uuid = Some(tag), matches = Set("uuid")))
    discourse.add(nodes: _*)
    discourse
  }

  private def addTagsToGraph(discourse: Discourse, user: User, node: Post) {
    discourse.tags.foreach(tag => {
      val categorizes = Categorizes.merge(tag, node)
      val action = TaggingAction.merge(user, categorizes)
      discourse.add(categorizes, action)
    })
  }

  //TODO: should create/update be nested?
  override def create(context: RequestContext) = {
    context.jsonAs[TaggedPostAddRequest].map(request => {
      val discourse = tagDefGraph(request.addedTags)

      val node = Post.create(title = request.title, description = request.description)
      val contribution = Created.create(context.user, node)
      discourse.add(node, contribution)

      addTagsToGraph(discourse, context.user, node)

      db.transaction(_.persistChanges(discourse)) match {
        case Some(err) => Left(s"Cannot create Post: $err'")
        case _         => Right(node)
      }
    }).getOrElse(Left("Error parsing create request for Tag"))
  }

  override def update(context: RequestContext, uuid: String) = {
    context.jsonAs[TaggedPostUpdateRequest].map(request => {
      val discourse = tagDefGraph(request.addedTags)

      val node = Post.matchesOnUuid(uuid)
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
        case Some(err) => Left(s"Cannot update Post with uuid '$uuid': $err'")
        case _         => Right(node)
      }
    }).getOrElse(Left("Error parsing update request for Tag"))
  }
}

object PostAccess {
  def apply = new PostAccess
}

class TagAccess extends NodeRead(TagLike) {
  override def create(context: RequestContext) = {
    context.jsonAs[TagAddRequest].map(request => {
      val node = Tag.merge(title = request.title, merge = Set("title"))
      val contribution = Created.create(context.user, node)

      val discourse = Discourse(node, contribution)
      db.transaction(_.persistChanges(discourse)) match {
        case Some(err) => Left(s"Cannot create Tag: $err'")
        case _         => Right(node)
      }
    }).getOrElse(Left("Error parsing create request for Tag"))
  }

  override def update(context: RequestContext, uuid: String) = {
    context.jsonAs[TagUpdateRequest].map(request => {
      val node = TagLike.matchesOnUuid(uuid)
      //TODO: normally we would want to set it back to None instead of ""
      if (request.description.isDefined) {
        node.description = request.description
      }

      val contribution = Updated.create(context.user, node)

      val discourse = Discourse(contribution)
      db.transaction(_.persistChanges(discourse)) match {
        case Some(err) => Left(s"Cannot update Tag with uuid '$uuid': $err'")
        case _         => Right(node)
      }
    }).getOrElse(Left("Error parsing update request for Tag"))
  }
}

object TagAccess {
  def apply = new TagAccess
}

class UserAccess extends NodeRead(User) {
  override def update(context: RequestContext, uuid: String) = {
    context.realUser.map { user =>
      context.jsonAs[UserUpdateRequest].map { request =>
        //TODO: sanity check + welcome mail
        if (request.email.isDefined)
          user.email = request.email

        db.transaction(_.persistChanges(user)) match {
          case Some(err) => Left(s"Cannot update User: $err'")
          case _         => Right(user)
        }
      }.getOrElse(Left("Error parsing update request for User"))
    }.getOrElse(Left("Cannot edit user"))
  }
}

object UserAccess {
  def apply = new UserAccess
}
