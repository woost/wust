import play.api.Logger
import play.api.{GlobalSettings, _}
import com.mohiva.play.silhouette.api.{Logger => SilhouetteLogger, SecuredSettings}
import play.api.i18n.Lang
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Results._
import play.api.mvc._
import play.filters.gzip.GzipFilter
import play.api.Play.current
import play.api.Play
import java.io.File
import com.typesafe.config.ConfigFactory

import scala.concurrent.Future

object LoggingFilter extends EssentialFilter {
  val red = "\u001b[31m"
  val yellow = "\u001b[33m"
  val reset = "\u001b[0m"

  def timeColored(text: String, ms: Long): String = {
    if (ms < 100) return text
    else if (ms < 1000) return s"$yellow$text$reset"
    else return s"$red$text$reset"
  }

  def statusColored(status: Int): String = {
    status match {
      case code if code < 400 => status.toString
      case x => s"$red$status$reset"
    }
  }

  def apply(nextFilter: EssentialAction) = new EssentialAction {
    def apply(requestHeader: RequestHeader) = {
      val startTime = System.nanoTime
      nextFilter(requestHeader).map { result =>
        val endTime = System.nanoTime
        val requestTime = (endTime - startTime) / 1000000
        val time = timeColored(s"${"%5d" format requestTime}ms", requestTime)
        Logger.info(s"$time ${requestHeader.method} ${requestHeader.uri} [${statusColored(result.header.status)}]")
        result.withHeaders("Request-Time" -> requestTime.toString)
      }
    }
  }
}

trait MyWithFilters extends GlobalSettings {
  // this reimplements the WithFilters class
  // https://github.com/playframework/playframework/blob/912ea929f61a5c452e090715e6d02a22871a0dbb/framework/src/play/src/main/scala/play/api/mvc/Filters.scala#L107
  lazy val filters = if (Play.isDev) {
    Array(LoggingFilter)
  } else {
    Array(new GzipFilter, LoggingFilter)
  }

  override def doFilter(a: EssentialAction): EssentialAction = {
    Filters(super.doFilter(a), filters: _*)
  }
}

object Global extends GlobalSettings with MyWithFilters with SecuredSettings with SilhouetteLogger {

  override def onNotAuthenticated(request: RequestHeader, lang: Lang): Option[Future[Result]] = {
    // Called when a user is not authenticated.
    // As defined by RFC 2616, the status code of the response should be 401 Unauthorized.
    // controllers.StaticResponse.onNotAuthenticated(request, lang)
    Some(Future { Unauthorized("credentials not correct") })
  }

  override def onNotAuthorized(request: RequestHeader, lang: Lang): Option[Future[Result]] = {
    // Called when a user is authenticated but not authorized.
    // As defined by RFC 2616, the status code of the response should be 403 Forbidden.
    // controllers.StaticResponse.onNotAuthorized(request, lang)
    Some(Future { Forbidden("credentials not correct") })
  }
}
