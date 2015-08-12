package modules.db.access.custom

import controllers.api.nodes.{HyperConnectParameter, RequestContext}
import modules.db.Database._
import modules.db.access.{AccessDecoratorControlDefault, AccessDecoratorControl, EndRelationAccessDefault}
import modules.requests.ConnectResponse
import play.api.mvc.Results._

// does not allow dummy users for any request
class CheckUser extends AccessDecoratorControl with AccessDecoratorControlDefault {
  override def acceptRequest(context: RequestContext) = {
    if (context.user.isDummy)
      Some(Unauthorized("Not Authorized"))
    else
      None
  }
}

object CheckUser {
  def apply = new CheckUser
}

// only allows read requests
class CheckUserWrite extends CheckUser {
  override def acceptRequestRead(context: RequestContext) = None
}

object CheckUserWrite {
  def apply = new CheckUserWrite
}
