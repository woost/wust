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
  def apply(nextFilter: EssentialAction) = new EssentialAction {
    def apply(requestHeader: RequestHeader) = {
      val startTime = System.currentTimeMillis
      nextFilter(requestHeader).map { result =>
        val endTime = System.currentTimeMillis
        val requestTime = endTime - startTime
        Logger.info(s"${"%5d" format requestTime}ms ${requestHeader.method} ${requestHeader.uri} [${result.header.status}]")
        result.withHeaders("Request-Time" -> requestTime.toString)
      }
    }
  }
}

trait MyWithFilters extends GlobalSettings {
  // this reimplements the WithFilters class
  // https://github.com/playframework/playframework/blob/912ea929f61a5c452e090715e6d02a22871a0dbb/framework/src/play/src/main/scala/play/api/mvc/Filters.scala#L107
  lazy val filters = if(Play.isDev) {
    Array(LoggingFilter)
  } else {
    Array(new GzipFilter)
  }

  override def doFilter(a: EssentialAction): EssentialAction = {
    Filters(super.doFilter(a), filters: _*)
  }
}

object Global extends GlobalSettings with MyWithFilters with SecuredSettings with SilhouetteLogger {

  wust.Shared.hello()

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

