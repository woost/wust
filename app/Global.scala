import org.atmosphere.cpr.ApplicationConfig
import org.atmosphere.play.AtmosphereCoordinator.{instance => atmosphere}
import org.atmosphere.play.{Router => atmosphereRouter}
import play.api.mvc.{Handler, RequestHeader}
import play.api._

object Global extends GlobalSettings {

  override def onStart(application: Application) {
    atmosphere.framework.addInitParameter(ApplicationConfig.WEBSOCKET_CONTENT_TYPE, "application/json")
    atmosphere.discover(classOf[live.Live])
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
