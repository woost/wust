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

object SeedReddit extends Task {
  val builder = new com.ning.http.client.AsyncHttpClientConfig.Builder()
  val ws = new play.api.libs.ws.ning.NingWSClient(builder.build())
  def getJson(url: String):JsValue = Await.result(ws.url(url).get(), 10.seconds).json

  val subreddit = "gue"
  val limit = 20
  println(s"importing comments from subreddit /r/$subreddit")
  val discourse = Discourse.empty

  try {
    val url = s"http://www.reddit.com/r/$subreddit/top.json?limit=$limit&t=all"
    val response = getJson(url)
    (response \ "data" \ "children") match {
      case JsArray(children) => children.foreach { post =>
        val id = (post \ "data" \ "id").as[String]
        val title = (post \ "data" \ "title").as[String]
        val content = (post \ "data" \ "selftext").as[String]
        val headPost = Post.local(title = Some(title), description = content)
        println(s"thread: $title")
        discourse.add(headPost)

        val comments = getJson(s"http://www.reddit.com/r/$subreddit/comments/$id.json").as[List[JsValue]].apply(1)
          addCommentsDeep(comments, headPost)

        def addCommentsDeep(comments:JsValue, parent:Post) {
          if((comments \ "kind").as[String] != "Listing") return;
          println(comments)
          (comments \ "data" \ "children") match {
            case JsArray(comments) => comments.filter(c => (c \ "kind").as[String] != "more").foreach { comment =>
              val body = (comment \ "data" \ "body").as[String]
              val title = body.take(100) + (if(body.size > 100) "..." else "")

              val commentNode = Post.local(title = Some(title), description = body)
              println(s" reply: $title")
              discourse.add(commentNode, Connects.local(commentNode, parent))

              val replies = (comment \ "data" \ "replies") match {
                case JsString("") =>
                case replies:JsValue => addCommentsDeep(replies, parent = commentNode)
              }
            }
          }
        }
      }
    }

    println("persisting...")
    db.persistChanges(discourse.graph)
  } catch {
    case e: Exception => e.printStackTrace()
  }

  ws.close()
  db.restService.actorSystem.shutdown()
}
