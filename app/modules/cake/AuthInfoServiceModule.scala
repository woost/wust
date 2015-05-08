package modules.cake

import com.mohiva.play.silhouette.api.util.PasswordInfo
import com.mohiva.play.silhouette.impl.daos.DelegableAuthInfoDAO
import com.mohiva.play.silhouette.impl.services.DelegableAuthInfoService

trait AuthInfoServiceModule {

  def passwordInfoDAO: DelegableAuthInfoDAO[PasswordInfo]

  lazy val authInfoService = new DelegableAuthInfoService(passwordInfoDAO)

}
