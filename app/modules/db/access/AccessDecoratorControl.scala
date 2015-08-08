package modules.db.access

import controllers.api.nodes.RequestContext
import play.api.mvc.Result

trait AccessDecoratorControlMethods {
  def acceptRequest(context: RequestContext): Option[Result]
  def acceptRequestRead(context: RequestContext): Option[Result]
  def acceptRequestWrite(context: RequestContext): Option[Result]
}

trait AccessDecoratorControl extends AccessDecoratorControlMethods {
  override def acceptRequest(context: RequestContext): Option[Result] = None
  override def acceptRequestRead(context: RequestContext): Option[Result] = acceptRequest(context)
  override def acceptRequestWrite(context: RequestContext): Option[Result] = acceptRequest(context)
}

trait AccessDecoratorControlForward extends AccessDecoratorControlMethods {
  val control: AccessDecoratorControl

  override def acceptRequest(context: RequestContext) = control.acceptRequest(context)
  override def acceptRequestRead(context: RequestContext) = control.acceptRequestRead(context)
  override def acceptRequestWrite(context: RequestContext) = control.acceptRequestWrite(context)
}
