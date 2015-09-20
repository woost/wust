package controllers.api.auth

import com.mohiva.play.silhouette.api.util.{PasswordInfo => SPasswordInfo}
import com.mohiva.play.silhouette.api.{Silhouette, _}
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import model.WustSchema.User
import model.auth.{SignUp, Token}
import modules.auth.HeaderEnvironmentModule
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.Future

class SignUpController extends Silhouette[User, JWTAuthenticator]
with HeaderEnvironmentModule {

  implicit val signUpFormat = Json.format[SignUp]

  def signUp = Action.async(parse.json) { implicit request =>
    request.body.validate[SignUp].map { signUp =>
      val loginInfo = LoginInfo(CredentialsProvider.ID, signUp.identifier)
      userService.retrieve(loginInfo).flatMap {
        case None    =>
          val authInfo = passwordHasher.hash(signUp.password)
          for {
            user <- userService.create(loginInfo, signUp)
            authInfo <- authInfoService.save(loginInfo, authInfo)
            authenticator <- env.authenticatorService.create(loginInfo)
            token <- env.authenticatorService.init(authenticator)
            result <- env.authenticatorService.embed(token, Future.successful {
              Ok(Json.toJson(Token(token = token, expiresOn = authenticator.expirationDate, userId = user.uuid)))
            })
          } yield {
            env.eventBus.publish(SignUpEvent(user, request, request2lang))
            env.eventBus.publish(LoginEvent(user, request, request2lang))
            mailService.sendWelcomeEmail(user)
            result
          }
        case Some(u) =>
          Future.successful(Conflict("user already exists"))
      }
    }.recoverTotal {
      case error =>
        Future.successful(BadRequest(error.toString))
    }
  }

  def signOut = SecuredAction.async { implicit request =>
    env.eventBus.publish(LogoutEvent(request.identity, request, request2lang))
    request.authenticator.discard(Future.successful(Ok))
  }
}

object SignUpController extends SignUpController
