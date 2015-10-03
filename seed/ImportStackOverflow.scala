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

object ImportStackOverflow extends Task with SeedTools {

  val builder = new com.ning.http.client.AsyncHttpClientConfig.Builder()
  builder.setCompressionEnabled(true)
  val ws = new play.api.libs.ws.ning.NingWSClient(builder.build())
  def getJson(url: String): JsValue = Await.result(ws.url(url).get(), 10.seconds).json

  val sites = List("vi","biology","math", "ux")
  val questionLimit = 10
  val filterId = "!9McUOAJfkgufsXMHUKprrtPH5vBjclpq8Z--RIM3a0bt2k)S-4zjE6n"
  val baseurl = s"http://api.stackexchange.com/2.2"

  val questionTag = mergeClassification("Question")
  val answerTag = mergeClassification("Answer")
  val soTag = mergeScope("StackExchange")
  //TODO: tag per site
  //TODO: ignore "untagged"

  def questions(site:String) = {
    println("downloading top questions...")
    val url = baseurl + s"/questions?page=1&pagesize=$questionLimit&order=desc&min=10&sort=votes&filter=$filterId&site=$site"
    val json = getJson(url)
    (json \ "items").as[Seq[JsObject]]
  }


  dbContext { implicit db =>
    val user = User.merge("StackOverflow")
    modifyDiscourse { discourse =>
      discourse.add(user)
    }

    def post(rawPost: JsObject)(implicit discourse:Discourse) = {
      val tagNames = (rawPost \ "tags").asOpt[Seq[String]]
      val body = (rawPost \ "body_markdown").as[String]
      val title = (rawPost \ "title").asOpt[String]
      val downVotes = (rawPost \ "down_vote_count").as[Long]
      val upVotes = (rawPost \ "up_vote_count").as[Long]
      val viewCount = (rawPost \ "view_count").asOpt[Long]
      val creationDate = (rawPost \ "creation_date").as[Long] * 1000

      val post = title.map(tit => createPost(tit, Some(body), creationDate)).getOrElse(createPost(body, Some(creationDate)))
      discourse.add(post)
      discourse.add(Created.create(user, post))
      tagNames.foreach(_.foreach(t => discourse.add(tag(post, mergeScope(t)))))

      //TODO: voting

      val comments = (rawPost \ "comments").asOpt[Seq[JsObject]]

      comments.foreach(_.foreach { rawComment =>
        val body = (rawComment \ "body").as[String]
        val score = (rawComment \ "score").as[Long]
        val creationDate = (rawComment \ "creation_date").as[Long] * 1000

        val comment = createPost(body, Some(creationDate))
        discourse.add(comment)
        discourse.add(Created.create(user, comment))

        val connects = Connects.create(comment, post)
        discourse.add(comment, connects)

        //TODO: voting
      })

      post
    }

    sites.foreach{ site =>
      println(s"Importing StackExchange: $site")
      questions(site).foreach { rawQuestion =>

        modifyDiscourse { implicit discourse =>
          val question = post(rawQuestion)
          println(s"Importing question: ${question.title}")
          val tags = tag(question, soTag)
          discourse.add(
            tags,
            classify(tags, questionTag)
          )

          (rawQuestion \ "answers").as[Seq[JsObject]].foreach { rawAnswer =>
            val answer = post(rawAnswer)
            val connects = Connects.create(answer, question)
            discourse.add(classify(connects, answerTag))
            discourse.add(answer, connects)
          }
        }
        Thread.sleep(1000)
      }
    }
  }


  ws.close()
}
