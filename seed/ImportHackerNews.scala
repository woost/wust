package tasks

import hackernews4s.v0._
import model.WustSchema._
import renesca.DbService
import renesca.parameter.implicits._

import scala.util.Try

object ImportHackerNews extends Task with TagTools {
  val hackerNewsScope = mergeScope(s"HackerNews")
  dbContext { implicit db =>
    println("merging HackerNews Tags...")
    mergeTags()

    println("merging HackerNews Scope...")
    modifyDiscourse { discourse =>
      discourse.add(hackerNewsScope)
    }

    val itemId: Option[ItemId] = None //Some(ItemId(9869886))
    if(itemId.isDefined) importItem(forceGetItem(itemId.get))
    else importTopStories()
  }

  def mergeTags()(implicit db: DbService) {
    modifyDiscourse { implicit discourse =>
      discourse.add(
        Inherits.merge(mergeTag("HN-Story"), hackerNewsScope),
        Inherits.merge(mergeTag("HN-Show"), hackerNewsScope),
        Inherits.merge(mergeTag("HN-Ask"), hackerNewsScope),
        Inherits.merge(mergeTag("HN-Comment"), hackerNewsScope),
        Inherits.merge(mergeTag("HN-Comment"), mergeTag("Comment"))
      )
    }
  }

  def importTopStories()(implicit db: DbService) {
    println("importing top Stories...")
    HackerNews.getItemIdsForTopStories().foreach { id =>
      importItem(forceGetItem(id))
    }
  }

  def forceGetItem(itemId: ItemId): Item = {
    var got: Option[Item] = None
    var sleep = 500 // exponential sleeping
    while(got.isEmpty) {
      Try {
        HackerNews.getItem(itemId).foreach { item => got = Some(item) }
        if(got.isEmpty) {
          print("*")
          Thread.sleep(sleep)
          sleep *= 2
        }
      }
    }
    got.get
  }

  def importItem(hnItem: Item)(implicit db: DbService): Unit = {
    modifyDiscourse { discourse =>
      println(s"importing ${ hnItem.itemType }: ${ hnItem.title.get }")
      val post = Post.create(hnItem.title.get, description = Some(hnItem.text))
      val commentTag = mergeTag("HN-Comment")
      val replyTag = mergeTag("repliesTo")
      discourse.add(
        tag(post, mergeTag(s"HN-${ hnItem.itemType }")),
          belongsTo(post, hackerNewsScope),
        commentTag
      )
      addDeepChildItems(hnItem, post)
      println()

      def addDeepChildItems(parentHnItem: Item, parentPost: Post): Unit = {
        parentHnItem.commentIds.foreach { itemId =>
          val hnItem = forceGetItem(itemId)
          val post = Post.create(title = hnItem.text.take(140), description = Some(hnItem.text))
          if(!hnItem.deleted) {
            if(post.validate.isDefined) {
              println("\nerror: " + post.validate.get)
              println("item: " + hnItem)
            } else {
              val connects = Connects.create(post, parentPost)
              discourse.add(post, connects, tag(post, commentTag), tag(connects, replyTag))
              print(".")
              addDeepChildItems(hnItem, post)
            }
          }
        }
      }
    }
  }
}
