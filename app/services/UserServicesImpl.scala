package services

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.services.AuthInfo
import com.mohiva.play.silhouette.impl.providers.CommonSocialProfile
import model.users._
import play.api.libs.concurrent.Execution.Implicits._
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
        case (id, user) => user.loginInfo == loginInfo || user.socials.map(_.find(li => li == loginInfo)).isDefined
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

  /**
   * Saves the social profile for a user.
   *
   * If a user exists for this profile then update the user, otherwise create a new user with the given profile.
   *
   * @param profile The social profile to save.
   * @return The user for whom the profile was saved.
   */
  def save[A <: AuthInfo](profile: CommonSocialProfile) = {
    play.Logger.debug {
      s"""UserServiceImpl.save ----------
        	------------------ profile: ${ profile }"""
    }
    retrieve(profile.loginInfo).flatMap {
      case Some(user) => // Update user with profile
        val u = user.copy(info = BaseInfo(
          firstName = profile.firstName,
          lastName = profile.lastName,
          fullName = profile.fullName,
          gender = None),
          email = profile.email
        )
        save(u)
      case None       => // Insert a new user
        val u = User(
          loginInfo = profile.loginInfo,
          username = None,
          info = BaseInfo(
            firstName = profile.firstName,
            lastName = profile.lastName,
            fullName = profile.fullName,
            gender = None),
          email = profile.email
        )
        save(u)
    }
  }

  /**
   * Link a social social profile on a user.
   *
   *
   * @param profile The social profile to save.
   * @return The user for whom the profile was saved.
   */
  def link[A <: AuthInfo](user: User, profile: CommonSocialProfile): Future[User] = {
    play.Logger.debug {
      s"""UserServiceImpl.link ----------
      		------------------ user: ${ user }
      		------------------ profile: ${ profile }
      		------------------ DB: ${ UserServiceImpl.users }"""
    }
    val s = user.socials.getOrElse(Seq())
    val u = user.copy(socials = Some(s :+ profile.loginInfo))
    save(u)
  }

}

object UserServiceImpl {
  val users: mutable.HashMap[String, User] = mutable.HashMap()
}
