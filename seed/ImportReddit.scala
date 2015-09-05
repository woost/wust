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

object ImportReddit extends Task with SeedTools {


  val builder = new com.ning.http.client.AsyncHttpClientConfig.Builder()
  val ws = new play.api.libs.ws.ning.NingWSClient(builder.build())
  def getJson(url: String): JsValue = Await.result(ws.url(url).get(), 10.seconds).json

  val redditScope = mergeScope("Reddit", color = Some(210))
  val replyTag = mergeClassification("repliesTo")

  dbContext { implicit db =>
    val subreddits = List("lifeprotips", "scala", "neo4j")
    val limit = 10

    for(subreddit <- subreddits) {
      println(s"importing comments from subreddit /r/$subreddit")
      // val url = s"http://www.reddit.com/r/$subreddit/top.json?limit=$limit&t=year"
      val url = s"http://www.reddit.com/r/$subreddit/hot.json?limit=$limit"

      println("merging subreddit Scope...")
      val subredditScope = mergeScope(s"/r/$subreddit")
      modifyDiscourse { discourse =>
        discourse.add(subredditScope, Inherits.create(subredditScope, redditScope))
      }

      val response = getJson(url)
      (response \ "data" \ "children": @unchecked) match {
        case JsArray(children) => children.foreach { post =>
          modifyDiscourse { implicit discourse =>
            val id = (post \ "data" \ "id").as[String]
            val title = (post \ "data" \ "title").as[String]
            val content = (post \ "data" \ "selftext").as[String]
            val url = (post \ "data" \ "url").as[String]
            val permalink = (post \ "data" \ "permalink").as[String]
            val startPost = createPost(title, content, if(url.endsWith(permalink)) None else Some(url))
            print(s"thread: $title")
            discourse.add(startPost, tag(startPost, subredditScope), replyTag, subredditScope)

            val jsonComments = getJson(s"http://www.reddit.com/r/$subreddit/comments/$id.json").as[List[JsValue]].apply(1)
            var commentCount = 0
            addCommentsDeep(jsonComments, startPost)
            println(s" ($commentCount comments)")

            def addCommentsDeep(jsonComments: JsValue, parent: Post) {
              if((jsonComments \ "kind").as[String] != "Listing") return;
              //              println(comments)
              (jsonComments \ "data" \ "children": @unchecked) match {
                case JsArray(comments) => comments.filter(c => (c \ "kind").as[String] != "more").foreach { comment =>
                  commentCount += 1
                  val commentPost = createPost((comment \ "data" \ "body").as[String])
                  val connects = Connects.create(commentPost, parent)
                  discourse.add(commentPost, connects, tag(commentPost, subredditScope), tag(connects, replyTag))

                  val replies = (comment \ "data" \ "replies": @unchecked) match {
                    case JsString("")     =>
                    case replies: JsValue => addCommentsDeep(replies, parent = commentPost)
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
