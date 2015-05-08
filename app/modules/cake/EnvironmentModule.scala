package modules.cake

import com.mohiva.play.silhouette.api.util.PlayHTTPLayer
import com.mohiva.play.silhouette.api.{Environment, EventBus}
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import com.mohiva.play.silhouette.impl.util.{BCryptPasswordHasher, PlayCacheLayer, SecureRandomIDGenerator}
import model.daos._
import model.users.User


trait HeaderEnvironmentModule
  extends HeaderAuthenticatorServiceModule
  with UserServiceModule
  with AuthInfoServiceModule
  with CredentialsProviderModule
  with MailServiceModule {

  lazy val cacheLayer = new PlayCacheLayer
  lazy val httpLayer = new PlayHTTPLayer
  lazy val eventBus = EventBus()
  lazy val idGenerator = new SecureRandomIDGenerator
  lazy val passwordInfoDAO = new PasswordInfoDAO
  lazy val passwordHasher = new BCryptPasswordHasher

  implicit lazy val env: Environment[User, JWTAuthenticator] = {
    Environment[User, JWTAuthenticator](
      userService,
      authenticatorService,
      Map(
        credentialsProvider.id -> credentialsProvider
      ),
      eventBus)
  }

}