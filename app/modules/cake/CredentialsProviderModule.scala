package modules.cake

import com.mohiva.play.silhouette.api.services.AuthInfoService
import com.mohiva.play.silhouette.api.util.PasswordHasher
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider

trait CredentialsProviderModule {

  def authInfoService: AuthInfoService
  def passwordHasher: PasswordHasher

  lazy val credentialsProvider = new CredentialsProvider(authInfoService, passwordHasher, Seq(passwordHasher))

}