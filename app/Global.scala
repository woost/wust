import org.atmosphere.cpr.{ApplicationConfig, AtmosphereFramework}
import org.atmosphere.play.AtmosphereCoordinator.{instance => atmosphere}
import org.atmosphere.play.Router
import play.api.mvc.Handler
import play.mvc.Http.RequestHeader
import play.{Application, GlobalSettings}

class Global extends GlobalSettings {

  override def onStart(application: Application) {
    atmosphere.framework.addInitParameter(ApplicationConfig.WEBSOCKET_CONTENT_TYPE, "application/json")
    atmosphere.discover(classOf[live.Live])
    atmosphere.ready()
  }

  override def onStop(application: Application) {
    atmosphere.shutdown()
  }

  // needed for atmosphere
  override def onRouteRequest(request: RequestHeader): Handler = {
    Router.dispatch(request)
  }
}
