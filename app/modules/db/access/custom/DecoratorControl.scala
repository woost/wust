package modules.db.access.custom

import controllers.api.nodes.RequestContext
import modules.db.access._
import play.api.mvc.Results._

// only permits access by loggedIn users
class CheckUser extends AccessDecoratorControl with AccessDecoratorControlDefault {
  override def acceptRequest(context: RequestContext, baseUuid: Option[String]) = {
    if(context.user.isEmpty)
      Some(Unauthorized("Not Authorized"))
    else
      None
  }
}

object CheckUser {
  def apply = new CheckUser
}

// only allows anonymous read requests
class CheckUserWrite extends CheckUser {
  override def acceptRequestRead(context: RequestContext, baseUuid: Option[String]) = None
}

object CheckUserWrite {
  def apply = new CheckUserWrite
}

// only allows request if baseUuid of request corresponds to the userid
class CheckOwnUser extends AccessDecoratorControl with AccessDecoratorControlDefault {
  override def acceptRequest(context: RequestContext, baseUuid: Option[String]) = context.user.map { user =>
    if (baseUuid.map(_ == user.uuid).getOrElse(false))
      None
    else
      Some(Unauthorized("Resource is private"))
  }.getOrElse(Some(context.onlyUsersError))
}

object CheckOwnUser {
  def apply = new CheckOwnUser
}
