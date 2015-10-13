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
      val hackerNewsScope = mergeScope("HackerNews")
      val startPost = if (hnItem.title.get.startsWith("Ask HN:")) {
        val startPost = createPost(hnItem.title.get.stripPrefix("Ask HN:"), hnItem.text, hnItem.url)
        val tags = tag(startPost, mergeScope("Ask-HN"))
        discourse.add(
          tags,
          classify(tags, matchClassification("Question")),
          Inherits.merge(mergeScope("Ask-HN"), hackerNewsScope)
        )
        startPost
      } else if (hnItem.title.get.startsWith("Show HN:")) {
        val startPost = createPost(hnItem.title.get.stripPrefix("Show HN:"), hnItem.text, hnItem.url)
        discourse.add(
          tag(startPost, mergeScope(s"Show-HN")),
          Inherits.merge(mergeScope(s"Show-HN"), hackerNewsScope)
        )
        startPost
      } else {
        val startPost = createPost(hnItem.title.get, hnItem.text, hnItem.url)
        discourse.add(
          tag(startPost, hackerNewsScope)
        )
        startPost
      }

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
              discourse.add(commentPost, connects)
              print(".")
              addDeepChildItems(hnItem, commentPost)
            }
          }
        }
      }
    }
  }
}
