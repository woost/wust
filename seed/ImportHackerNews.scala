package tasks

import hackernews4s.v0._
import model.WustSchema._
import renesca.DbService
import renesca.parameter.implicits._

import scala.util.Try

object ImportHackerNews extends Task {
  dbContext { implicit db =>
    val itemId: Option[ItemId] = None //Some(ItemId(9869886))
    if(itemId.isDefined) {
      importItem(forceGetItem(itemId.get))
    } else {
      importTopStories()
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
    while(got.isEmpty) {
      Try {
        HackerNews.getItem(itemId).foreach { item => got = Some(item) }
        if(got.isEmpty) {
          print("*")
          Thread.sleep(1000)
        }
      }
    }
    got.get
  }

  def importItem(hnItem: Item)(implicit db: DbService): Unit = {
    modifyDiscourse { discourse =>
      println(s"importing ${ hnItem.itemType }: ${ hnItem.title.get }")
      val post = Post.create(hnItem.title.get, description = Some(hnItem.text))
      discourse.add(Categorizes.create(Tag.matches(title = Some(s"HN-${ hnItem.itemType }"), matches = Set("title")), post))
      addDeepChildItems(hnItem, post)
      println()

      def addDeepChildItems(parentHnItem: Item, parentPost: Post): Unit = {
        parentHnItem.commentIds.foreach { itemId =>
          val hnItem = forceGetItem(itemId)
          val post = Post.create(title = hnItem.text.take(140), description = Some(hnItem.text))
          discourse.add(Connects.create(post, parentPost))
          print(".")
          addDeepChildItems(hnItem, post)
        }
      }
    }
  }
}
