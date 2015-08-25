import play.api.Logger
import play.api.{GlobalSettings, _}
import com.mohiva.play.silhouette.api.{Logger => SilhouetteLogger, SecuredSettings}
import play.api.i18n.Lang
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Results._
import play.api.mvc._
import play.filters.gzip.GzipFilter
import play.api.Play.current
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

object DevelopmentFilters extends WithFilters(LoggingFilter)
object ProductionFilters extends WithFilters(new GzipFilter())

object Global extends GlobalSettings with SecuredSettings with SilhouetteLogger {

  wust.Shared.hello()

  override def onLoadConfig(config: Configuration, path: File, classloader: ClassLoader, mode: Mode.Mode): Configuration = {
    val modeSpecificConfig = config ++ Configuration(ConfigFactory.load(s"application.${mode.toString.toLowerCase}.conf"))
    super.onLoadConfig(modeSpecificConfig, path, classloader, mode)
  }

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

