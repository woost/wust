package modules.auth

import com.mohiva.play.silhouette.api.services.AuthenticatorService
import com.mohiva.play.silhouette.api.util.{Clock, PlayHTTPLayer}
import com.mohiva.play.silhouette.api.{Environment, EventBus}
import com.mohiva.play.silhouette.impl.authenticators.{JWTAuthenticator, _}
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import com.mohiva.play.silhouette.impl.services.DelegableAuthInfoService
import com.mohiva.play.silhouette.impl.util.{BCryptPasswordHasher, PlayCacheLayer, SecureRandomIDGenerator}
import model.WustSchema.User
import model.auth.PasswordInfoDAO
import play.api.Play
import play.api.Play.current
import services.{SimpleMailService, UserServiceDB}


trait HeaderEnvironmentModule {

  lazy val cacheLayer = new PlayCacheLayer
  lazy val httpLayer = new PlayHTTPLayer
  lazy val eventBus = EventBus()
  lazy val idGenerator = new SecureRandomIDGenerator
  lazy val passwordInfoDAO = new PasswordInfoDAO
  lazy val passwordHasher = new BCryptPasswordHasher

  lazy val userService = new UserServiceDB

  lazy val credentialsProvider = new CredentialsProvider(authInfoService, passwordHasher, Seq(passwordHasher))

  lazy val authInfoService = new DelegableAuthInfoService(passwordInfoDAO)

  lazy val mailService = new SimpleMailService

  lazy val authenticatorService: AuthenticatorService[JWTAuthenticator] = {
    val settings = JWTAuthenticatorSettings(
      headerName = Play.configuration.getString("silhouette.authenticator.headerName").getOrElse { "X-Auth-Token" },
      issuerClaim = Play.configuration.getString("silhouette.authenticator.issueClaim").getOrElse { "play-silhouette" },
      encryptSubject = Play.configuration.getBoolean("silhouette.authenticator.encryptSubject").getOrElse { true },
      authenticatorIdleTimeout = Play.configuration.getInt("silhouette.authenticator.authenticatorIdleTimeout"), // This feature is disabled by default to prevent the generation of a new JWT on every request
      authenticatorExpiry = Play.configuration.getInt("silhouette.authenticator.authenticatorExpiry").getOrElse { 12 * 60 * 60 },
      sharedSecret = Play.configuration.getString("application.secret").get)
    new JWTAuthenticatorService(
      settings = settings,
      dao = None,
      idGenerator = idGenerator,
      clock = Clock())
  }

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