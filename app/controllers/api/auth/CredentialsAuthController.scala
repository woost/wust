package controllers.api.auth

import com.mohiva.play.silhouette.api.exceptions.AuthenticatorException
import com.mohiva.play.silhouette.api.util.Credentials
import com.mohiva.play.silhouette.api.{LoginInfo => SLoginInfo, Silhouette, _}
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import formatters.json.CredentialFormat
import model.WustSchema.User
import model.auth.Token
import modules.auth.HeaderEnvironmentModule
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.Future

class CredentialsAuthController extends Silhouette[User, JWTAuthenticator]
  with HeaderEnvironmentModule {

  implicit val restCredentialFormat = CredentialFormat.restFormat

  def authenticate = Action.async(parse.json) { implicit request =>
    request.body.validate[Credentials].map { credentials =>
      (env.providers.get(CredentialsProvider.ID) match {
        case Some(p: CredentialsProvider) => p.authenticate(credentials)
        case _                            => Future.failed(new AuthenticatorException(s"Cannot find credentials provider"))
      }).flatMap { loginInfo =>
        userService.retrieve(loginInfo).flatMap {
          case Some(user) => env.authenticatorService.create(loginInfo).flatMap { authenticator =>
            env.eventBus.publish(LoginEvent(user, request, request2lang))
            env.authenticatorService.init(authenticator).flatMap { token =>
              env.authenticatorService.embed(token, Future.successful {
                Ok(Json.toJson(Token(token = token, expiresOn = authenticator.expirationDate, userId = user.uuid)))
              })
            }
          }
          case None =>
            Future.failed(new AuthenticatorException("Couldn't find user"))
        }
      }.recoverWith(exceptionHandler)
    }.recoverTotal {
      case error => Future.successful(BadRequest(error.toString))
    }
  }

}

object CredentialsAuthController extends CredentialsAuthController
