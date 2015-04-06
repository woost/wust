package services

import play.api.{Logger, Application}
import securesocial.core._
import securesocial.core.providers.{UsernamePasswordProvider, MailToken}
import scala.concurrent.Future
import securesocial.core.services.{UserService, SaveMode}
import model.DemoUser
import org.joda.time.format.{DateTimeFormatter, DateTimeFormat}

class DemoUserService extends UserService[DemoUser] {
  val logger = Logger("application.controllers.UserService")

  var users = Map[(String, String), DemoUser]()
  //private var identities = Map[String, BasicProfile]()
  private var tokens = Map[String, MailToken]()

  def find(providerId: String, userId: String): Future[Option[BasicProfile]] = {
    if(logger.isDebugEnabled) {
      logger.debug("users = %s".format(users))
    }
    val result = for(
      user <- users.values;
      basicProfile <- user.identities.find(su => su.providerId == providerId && su.userId == userId)
    ) yield {
        basicProfile
      }
    Future.successful(result.headOption)
  }

  def findByEmailAndProvider(email: String, providerId: String): Future[Option[BasicProfile]] = {
    if(logger.isDebugEnabled) {
      logger.debug("users = %s".format(users))
    }
    val someEmail = Some(email)
    val result = for(
      user <- users.values;
      basicProfile <- user.identities.find(su => su.providerId == providerId && su.email == someEmail)
    ) yield {
        basicProfile
      }
    Future.successful(result.headOption)
  }

  def save(user: BasicProfile, mode: SaveMode): Future[DemoUser] = {
    mode match {
      case SaveMode.SignUp   =>
        val newUser = DemoUser(user, List(user))
        users = users + ((user.providerId, user.userId) -> newUser)
      case SaveMode.LoggedIn =>

    }
    // first see if there is a user with this BasicProfile already.
    val maybeUser = users.find {
      case (key, value) if value.identities.exists(su => su.providerId == user.providerId && su.userId == user.userId) => true
      case _                                                                                                           => false
    }
    maybeUser match {
      case Some(existingUser) =>
        val identities = existingUser._2.identities
        val updatedList = identities.patch(identities.indexWhere(i => i.providerId == user.providerId && i.userId == user.userId), Seq(user), 1)
        val updatedUser = existingUser._2.copy(identities = updatedList)
        users = users + (existingUser._1 -> updatedUser)
        Future.successful(updatedUser)

      case None =>
        val newUser = DemoUser(user, List(user))
        users = users + ((user.providerId, user.userId) -> newUser)
        Future.successful(newUser)
    }
  }

  def link(current: DemoUser, to: BasicProfile): Future[DemoUser] = {
    if(current.identities.exists(i => i.providerId == to.providerId && i.userId == to.userId)) {
      Future.successful(current)
    } else {
      val added = to :: current.identities
      val updatedUser = current.copy(identities = added)
      users = users + ((current.main.providerId, current.main.userId) -> updatedUser)
      Future.successful(updatedUser)
    }
  }

  def saveToken(token: MailToken): Future[MailToken] = {
    Future.successful {
      tokens += (token.uuid -> token)
      token
    }
  }

  def findToken(token: String): Future[Option[MailToken]] = {
    Future.successful {
      tokens.get(token)
    }
  }

  def deleteToken(uuid: String): Future[Option[MailToken]] = {
    Future.successful {
      tokens.get(uuid) match {
        case Some(token) =>
          tokens -= uuid
          Some(token)
        case None        => None
      }
    }
  }

  def deleteExpiredTokens() {
    tokens = tokens.filter(!_._2.isExpired)
  }

  override def updatePasswordInfo(user: DemoUser, info: PasswordInfo): Future[Option[BasicProfile]] = {
    Future.successful {
      for(
        found <- users.values.find(_ == user);
        identityWithPasswordInfo <- found.identities.find(_.providerId == UsernamePasswordProvider.UsernamePassword)
      ) yield {
        val idx = found.identities.indexOf(identityWithPasswordInfo)
        val updated = identityWithPasswordInfo.copy(passwordInfo = Some(info))
        val updatedIdentities = found.identities.patch(idx, Seq(updated), 1)
        found.copy(identities = updatedIdentities)
        updated
      }
    }
  }

  override def passwordInfoFor(user: DemoUser): Future[Option[PasswordInfo]] = {
    Future.successful {
      for(
        found <- users.values.find(_ == user);
        identityWithPasswordInfo <- found.identities.find(_.providerId == UsernamePasswordProvider.UsernamePassword)
      ) yield {
        identityWithPasswordInfo.passwordInfo.get
      }
    }
  }
}
