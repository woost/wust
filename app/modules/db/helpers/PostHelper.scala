package modules.db.helpers

import model.WustSchema._
import modules.db.Database.db
import modules.db._
import modules.requests._
import renesca.parameter.implicits._
import wust.Shared.tagTitleColor
import scala.concurrent._
import ExecutionContext.Implicits.global

object PostHelper {
  def tagConnectRequestToScope(tag: TagConnectRequest) = {
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

  def addScopesToGraph(discourse: Discourse, request: PostAddRequest, node: Post) {
    request.addedTags.flatMap(req => tagConnectRequestToScope(req).map((req, _))).foreach { case (req, tag) =>
      val tags = Tags.merge(tag, node)
      discourse.add(tags)
      req.classifications.map(c => Classification.matchesOnUuid(c.id)).foreach { classification =>
        discourse.add(Classifies.merge(classification, tags))
      }
    }
  }

  def createPost(request: PostAddRequest, user: User): Discourse = {
    val node = Post.create(title = request.title, description = request.description)
    val contribution = Created.create(user, node)
    val viewed = Viewed.create(user, node)
    node.viewCount = 1

    val discourse = Discourse(viewed, contribution)
    addScopesToGraph(discourse, request, node)

    PostHelper.viewPost(node, user)

    discourse
  }


  def viewPost(node: Post, user: User) = Future {
    db.transaction { tx =>
      implicit val ctx = new QueryContext
      val postDef = ConcreteNodeDef(node)
      val userDef = ConcreteNodeDef(user)
      val viewedDef = RelationDef(userDef, Viewed, postDef)
      val query = s"""
      match ${postDef.toPattern}
      set ${postDef.name}._locked = true
      with ${postDef.name}
      optional match ${viewedDef.toPattern(true, false)}
      return *
      """

      val discourse = Discourse(tx.queryGraph(query, ctx.params))
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
