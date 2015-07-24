package tasks

import common.ConfigString._
import model.WustSchema._
import modules.db.Database
import play.core.StaticApplication
import renesca._
import java.io.File
import play.api.Play.current
import play.api.libs.ws.ning.NingAsyncHttpClientConfigBuilder
import scala.concurrent.{Future, Await}
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.json._
import scala.util.Try
import scala.concurrent.duration._

object ImportReddit extends Task with TagTools {
  val builder = new com.ning.http.client.AsyncHttpClientConfig.Builder()
  val ws = new play.api.libs.ws.ning.NingWSClient(builder.build())
  def getJson(url: String): JsValue = Await.result(ws.url(url).get(), 10.seconds).json

  val redditScope = mergeScope("Reddit")
  val commentTag = mergeTag("Reddit-Comment")
  val startPostTag = mergeTag("Reddit-StartPost")
  val replyTag = mergeTag("repliesTo")

  def mergeTags()(implicit db: DbService) {
    println("merging Reddit tags...")
    modifyDiscourse { implicit discourse =>
      discourse.add(
        Inherits.merge(startPostTag, redditScope),
        Inherits.merge(commentTag, redditScope),
        Inherits.merge(commentTag, mergeTag("Comment"))
      )
    }
  }

  dbContext { implicit db =>
    val subreddits = List("neo4j", "scala", "lifeprotips")
    val limit = 20

    mergeTags()

    for(subreddit <- subreddits) {
      println(s"importing comments from subreddit /r/$subreddit")
      // val url = s"http://www.reddit.com/r/$subreddit/top.json?limit=$limit&t=week"
      val url = s"http://www.reddit.com/r/$subreddit/hot.json?limit=$limit"

      println("merging subreddit Scope...")
      val subredditScope = mergeScope(s"/r/$subreddit")
      modifyDiscourse { discourse =>
        discourse.add(subredditScope, Inherits.create(subredditScope, redditScope))
      }

      val response = getJson(url)
      (response \ "data" \ "children") match {
        case JsArray(children) => children.foreach { post =>
          modifyDiscourse { implicit discourse =>
            val id = (post \ "data" \ "id").as[String]
            val title = (post \ "data" \ "title").as[String]
            val content = (post \ "data" \ "selftext").as[String]
            val startPost = Post.create(title = title.take(140), description = Some(content))
            println(s"thread: $title")
            discourse.add(startPost, belongsTo(startPost, subredditScope), tag(startPost, startPostTag), commentTag, replyTag, subredditScope)

            val comments = getJson(s"http://www.reddit.com/r/$subreddit/comments/$id.json").as[List[JsValue]].apply(1)
            addCommentsDeep(comments, startPost)

            def addCommentsDeep(comments: JsValue, parent: Post) {
              if((comments \ "kind").as[String] != "Listing") return;
              //              println(comments)
              (comments \ "data" \ "children") match {
                case JsArray(comments) => comments.filter(c => (c \ "kind").as[String] != "more").foreach { comment =>
                  val body = (comment \ "data" \ "body").as[String]
                  val title = body.take(100) + (if(body.size > 100) "..." else "")

                  //                  println(s"comment: $title")
                  val commentNode = Post.create(title = title.take(140), description = Some(body))
                  val connects = Connects.create(commentNode, parent)
                  discourse.add(commentNode, connects, tag(commentNode, commentTag), tag(connects, replyTag))

                  val replies = (comment \ "data" \ "replies") match {
                    case JsString("")     =>
                    case replies: JsValue => addCommentsDeep(replies, parent = commentNode)
                  }
                }
              }
            }
          }
        }
      }
    }

    ws.close()
  }
}
