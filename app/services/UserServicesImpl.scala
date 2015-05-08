package services

import com.mohiva.play.silhouette.api.LoginInfo
import model.users._
import play.api.libs.json._
import security.models.SignUp

import scala.collection.mutable
import scala.concurrent.Future

class UserServiceInMemory extends UserService {

  /**
   * Create a user from login information and signup information
   *
   * @param loginInfo The information about login
   * @param signUp The information about User
   * @param json all json with signup information
   */
  def create(loginInfo: LoginInfo, signUp: SignUp, json: JsValue = JsNull): Future[User] = {
    val fullName = signUp.fullName.getOrElse(signUp.firstName.getOrElse("None") + " " + signUp.lastName.getOrElse("None"))
    val info = BaseInfo(
      firstName = signUp.firstName,
      lastName = signUp.lastName,
      fullName = Some(fullName),
      gender = None)
    val user = User(
      loginInfo = loginInfo,
      email = Some(signUp.identifier),
      username = None,
      info = info)
    Future.successful {
      User(
        loginInfo = loginInfo,
        email = Some(signUp.identifier),
        username = None,
        info = info)
    }
  }

  /**
   * Retrieves a user that matches the specified login info.
   *
   * @param loginInfo The login info to retrieve a user.
   * @return The retrieved user or None if no user could be retrieved for the given login info.
   */
  def retrieve(loginInfo: LoginInfo): Future[Option[User]] = {
    play.Logger.debug {
      s"""UserServiceImpl.retrieve ----------
      		------------------ loginInfo: ${ loginInfo }
      		------------------ DB: ${ UserServiceImpl.users }"""
    }
    Future.successful {
      UserServiceImpl.users.find {
        case (id, user) => user.loginInfo == loginInfo
      }.map(_._2)
    }
  }

  /**
   * Saves a user.
   *
   * @param user The user to save.
   * @return The saved user.
   */
  def save(user: User) = {
    play.Logger.debug {
      s"""UserServiceImpl.save ----------
      		------------------ user: ${ user }"""
    }
    UserServiceImpl.users += (user.loginInfo.toString -> user)
    Future.successful(user)
  }
}

object UserServiceImpl {
  val users: mutable.HashMap[String, User] = mutable.HashMap()
}
