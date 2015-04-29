import org.atmosphere.cpr.ApplicationConfig
import org.atmosphere.play.AtmosphereCoordinator.{instance => atmosphere}
import play.api.{GlobalSettings, _}
import com.mohiva.play.silhouette.api.{Logger, SecuredSettings}
import org.atmosphere.cpr.ApplicationConfig
import org.atmosphere.play.AtmosphereCoordinator.{instance => atmosphere}
import org.atmosphere.play.{Router => atmosphereRouter}
import play.api.{GlobalSettings, _}
import play.api.i18n.Lang
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Results._
import play.api.mvc.{Handler, RequestHeader, Result}

import scala.concurrent.Future

object Global extends GlobalSettings with SecuredSettings with Logger {

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
  override def onStart(application: Application) {
    atmosphere.framework.addInitParameter(ApplicationConfig.WEBSOCKET_CONTENT_TYPE, "application/json")
    atmosphere.discover(classOf[modules.live.Live])
    atmosphere.ready()
  }

  override def onStop(application: Application) {
    atmosphere.shutdown()
  }

  override def onRouteRequest(request: RequestHeader): Option[Handler] = {
    atmosphereRouter.dispatch(request) match {
      case Some(result) => Some(result)
      case None         => super.onRouteRequest(request)
    }
  }
}
