package tasks

import hackernews4s.v0._
import model.WustSchema._
import renesca.DbService
import renesca.parameter.implicits._

import scala.util.Try

object ImportHackerNews extends Task with SeedTools {

  val hackerNewsScope = mergeScope(s"HackerNews", color = Some(24))
  dbContext { implicit db =>
    println("merging HackerNews Scope...")
    modifyDiscourse { discourse =>
      discourse.add(hackerNewsScope)
    }

    val itemId: Option[ItemId] = None //Some(ItemId(9869886))
    if(itemId.isDefined) importItem(forceGetItem(itemId.get))
    else {
      importTopQuestions()
      importTopStories()
    }
  }

  def importTopStories()(implicit db: DbService) {
    println("importing top Stories...")
    HackerNews.getItemIdsForTopStories().take(10).foreach { id =>
      importItem(forceGetItem(id))
    }
  }

  def importTopQuestions()(implicit db: DbService) {
    println("importing top Questions...")
    HackerNews.getItemIdsForAskStories().take(10).foreach { id =>
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
      val startPost = createPost(hnItem.title.get, hnItem.text, hnItem.url)
      val startPostTag = mergeStaticTag("StartPost")
      val replyTag = mergeClassification("repliesTo")
      val hackerNewsScope = mergeScope("HackerNews")
      discourse.add(
        tag(startPost, mergeStaticTag(s"HN-${ hnItem.itemType }")),
        tag(startPost, hackerNewsScope),
        tag(startPost, startPostTag),
        Inherits.merge(mergeStaticTag(s"HN-${ hnItem.itemType }"), hackerNewsScope)
      )
      if(hnItem.itemType == "Ask")
        discourse.add(tag(startPost, mergeClassification("Question")))

      addDeepChildItems(hnItem, startPost)
      println()

      def addDeepChildItems(parentHnItem: Item, parentPost: Post): Unit = {
        parentHnItem.commentIds.foreach { itemId =>
          val hnItem = forceGetItem(itemId)
          val commentPost = createPost(hnItem.text)
          if(!hnItem.deleted) {
            if(commentPost.validate.isDefined) {
              println("\nerror: " + commentPost.validate.get)
              println("item: " + hnItem)
            } else {
              val connects = Connects.create(commentPost, parentPost)
              discourse.add(commentPost, connects, tag(commentPost, hackerNewsScope), tag(connects, replyTag))
              print(".")
              addDeepChildItems(hnItem, commentPost)
            }
          }
        }
      }
    }
  }
}
