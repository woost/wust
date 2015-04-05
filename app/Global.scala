import org.atmosphere.cpr.ApplicationConfig
import org.atmosphere.play.AtmosphereCoordinator.{instance => atmosphere}
import org.atmosphere.play.{Router => atmosphereRouter}
import play.api.mvc.{Handler, RequestHeader}
import play.api._
import java.lang.reflect.Constructor
import securesocial.core.RuntimeEnvironment
import securesocial.core.providers._
import securesocial.core.providers.utils.{Mailer, PasswordHasher, PasswordValidator}
import services.DemoUserService
import model.DemoUser
import scala.collection.immutable.ListMap

object Global extends GlobalSettings {

  object DemoRuntimeEnvironment extends RuntimeEnvironment.Default[DemoUser] {
    override lazy val userService: DemoUserService = new DemoUserService
    override lazy val providers = ListMap(
      include(new FacebookProvider(routes, cacheService, oauth2ClientFor(FacebookProvider.Facebook))),
      include(new GitHubProvider(routes, cacheService, oauth2ClientFor(GitHubProvider.GitHub))),
      include(new GoogleProvider(routes, cacheService, oauth2ClientFor(GoogleProvider.Google))),
      include(new LinkedInProvider(routes, cacheService, oauth1ClientFor(LinkedInProvider.LinkedIn))),
      include(new TwitterProvider(routes, cacheService, oauth1ClientFor(TwitterProvider.Twitter))),
      include(new UsernamePasswordProvider[DemoUser](userService, avatarService, viewTemplates, passwordHashers))
    )
  }

  // Dependency injection on Controllers using Cake Pattern
  override def getControllerInstance[A](controllerClass: Class[A]): A = {
    val instance = controllerClass.getConstructors.find { c =>
      val params = c.getParameterTypes
      params.length == 1 && params(0) == classOf[RuntimeEnvironment[DemoUser]]
    }.map {
      _.asInstanceOf[Constructor[A]].newInstance(DemoRuntimeEnvironment)
    }
    instance.getOrElse(super.getControllerInstance(controllerClass))
  }

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
