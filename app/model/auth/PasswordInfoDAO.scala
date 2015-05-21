package model.auth

import com.mohiva.play.silhouette.api.{LoginInfo => SLoginInfo}
import com.mohiva.play.silhouette.api.util.{PasswordInfo => SPasswordInfo}
import com.mohiva.play.silhouette.impl.daos.DelegableAuthInfoDAO
import model.WustSchema._
import modules.db.Database
import renesca.Query
import renesca.parameter.implicits._
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._

class PasswordInfoDAO extends DelegableAuthInfoDAO[SPasswordInfo] {
  implicit def sPasswordInfoToPasswordInfo(pi: SPasswordInfo) = PasswordInfo.local(pi.hasher, pi.password, pi.salt)
  implicit def passwordInfoToSPasswordInfo(pi: PasswordInfo) = SPasswordInfo(pi.hasher, pi.password, pi.salt)

  def save(sLoginInfo: SLoginInfo, sPasswordInfo: SPasswordInfo): Future[SPasswordInfo] = Future {
    val auth = Auth(Database.db.queryGraph(
      Query(s"match (l:`${LoginInfo.label}` {providerKey: {providerKey}, providerID: {providerID}}) return l",
        Map("providerKey" -> sLoginInfo.providerKey, "providerID" -> sLoginInfo.providerID))))
    auth.loginInfos.headOption match {
      case Some(loginInfo) => {
        val passwordInfo: PasswordInfo = sPasswordInfo
        val hasPassword = HasPassword.local(loginInfo, passwordInfo)
        auth.add(passwordInfo, hasPassword)
        Database.db.persistChanges(auth.graph)
        sPasswordInfo
      }
      case None => throw new Exception(s"Cannot find LoginInfo: ${sLoginInfo}")
    }
  }

  def find(sLoginInfo: SLoginInfo): Future[Option[SPasswordInfo]] = Future {
    val auth = Auth(Database.db.queryGraph(
      Query(s"match (l:`${LoginInfo.label}` {providerKey: {providerKey}, providerID: {providerID}})-[r:`${HasPassword.relationType}`]->(p:`${PasswordInfo.label}`) return p",
        Map("providerKey" -> sLoginInfo.providerKey, "providerID" -> sLoginInfo.providerID))))
    auth.passwordInfos.headOption.flatMap(Some(_))
  }
}
