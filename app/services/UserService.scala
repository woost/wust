package services

import com.mohiva.play.silhouette.api.{LoginInfo => SLoginInfo}
import com.mohiva.play.silhouette.api.services.IdentityService
import model.WustSchema.{Auth, User, LoginInfo, HasLogin}
import model.auth.SignUp
import modules.db.Database
import play.api.libs.json.{JsNull, JsValue}
import renesca.Query
import renesca.parameter.implicits._

import scala.concurrent.Future

trait UserService extends IdentityService[User] {

  def create(loginInfo: SLoginInfo, signUp: SignUp, json: JsValue = JsNull): Future[User]

}

class UserServiceDB extends UserService {
  implicit def sloginInfoToLoginInfo(li: SLoginInfo) = LoginInfo.local(li.providerID, li.providerKey)

  def create(sLoginInfo: SLoginInfo, signUp: SignUp, json: JsValue = JsNull): Future[User] = {
    val user = User.local(signUp.identifier)
    val loginInfo: LoginInfo = sLoginInfo
    val hasLogin = HasLogin.local(user, loginInfo)
    val auth = Auth.empty
    auth.add(user, loginInfo, hasLogin)
    Database.db.persistChanges(auth.graph)
    Future.successful(user)
  }

  def retrieve(sLoginInfo: SLoginInfo): Future[Option[User]] = {
    val auth = Auth(Database.db.queryGraph(
      Query(s"match (u:`${User.label}`)-[r:`${HasLogin.relationType}`]->(l:`${LoginInfo.label}` {providerKey: {providerKey}, providerID: {providerID}}) return u",
        Map("providerKey" -> sLoginInfo.providerKey, "providerID" -> sLoginInfo.providerID))))
    Future.successful(auth.users.headOption)
  }

  def retrieve: Future[Set[User]] = {
    val auth = Auth(Database.db.queryGraph(s"match (u:`${User.label}`) return u"))
    Future.successful(auth.users)
  }

  def retrieve(uuid: String): Future[Option[User]] = {
    val auth = Auth(Database.db.queryGraph(Query(s"match (u:`${User.label}` {uuid: {uuid}}) return u limit 1",
      Map("uuid" -> uuid))))
    Future.successful(auth.users.headOption)
  }
}
