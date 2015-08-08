package modules.db.access.custom

import controllers.api.nodes.{HyperConnectParameter, RequestContext}
import modules.db.Database._
import modules.db.access.{AccessDecoratorControl, EndRelationAccessDefault}
import modules.requests.ConnectResponse

class CheckUser extends AccessDecoratorControl {
  override def acceptRequest(context: RequestContext) = {
    if (context.user.isDummy)
      //TODO: status
      Some("Not Authorized")
    else
      None
  }
}

object CheckUser {
  def apply = new CheckUser
}