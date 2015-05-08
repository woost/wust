package controllers.security

import com.mohiva.play.silhouette.api.{Silhouette, _}
import com.mohiva.play.silhouette.api.exceptions.AuthenticatorException
import com.mohiva.play.silhouette.api.util.Credentials
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import model.users.User
import modules.cake.HeaderEnvironmentModule
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json._
import play.api.mvc._
import security.models._

import scala.concurrent.Future

/**
 * This controller manage authentication of an user by identifier and password
 */
class CredentialsAuthController extends Silhouette[User, JWTAuthenticator]
  with HeaderEnvironmentModule {

  /**
   *
   */
  implicit val restCredentialFormat = security.formatters.json.CredentialFormat.restFormat

  /**
   * Authenticates a user against the credentials provider.
   *
   * receive json like this:
   * {
   * 	"identifier": "...",
   *  	"password": "..."
   * }
   *
   * @return The result to display.
   */
  def authenticate = Action.async(parse.json) { implicit request =>
    request.body.validate[Credentials].map { credentials =>
      (env.providers.get(CredentialsProvider.ID) match {
        case Some(p: CredentialsProvider) => p.authenticate(credentials)
        case _                            => Future.failed(new AuthenticatorException(s"Cannot find credentials provider"))
      }).flatMap { loginInfo =>
        userService.retrieve(loginInfo).flatMap {
          case Some(user) => env.authenticatorService.create(user.loginInfo).flatMap { authenticator =>
            env.eventBus.publish(LoginEvent(user, request, request2lang))
            env.authenticatorService.init(authenticator).flatMap { token =>
              env.authenticatorService.embed(token, Future.successful {
                Ok(Json.toJson(Token(token = token, expiresOn = authenticator.expirationDate)))
              })
            }
          }
          case None =>
            Future.failed(new AuthenticatorException("Couldn't find user"))
        }
      }.recoverWith(exceptionHandler)
    }.recoverTotal {
      case error => Future.successful(BadRequest(Json.obj("message" -> JsError.toFlatJson(error))))
    }
  }

}

object CredentialsAuthController extends CredentialsAuthController
