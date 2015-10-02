package modules.db.access

import controllers.api.nodes.RequestContext
import play.api.mvc.Result

trait AccessDecoratorControlMethods {
  def acceptRequest(context: RequestContext, baseUuid: Option[String]): Option[Result]
  def acceptRequestRead(context: RequestContext, baseUuid: Option[String]): Option[Result]
  def acceptRequestWrite(context: RequestContext, baseUuid: Option[String]): Option[Result]
  def acceptRequestDelete(context: RequestContext, baseUuid: Option[String]): Option[Result]
}

trait AccessDecoratorControl extends AccessDecoratorControlMethods

trait AccessDecoratorControlDefault extends AccessDecoratorControlMethods {
  def acceptRequest(context: RequestContext, baseUuid: Option[String]): Option[Result] = None
  def acceptRequestRead(context: RequestContext, baseUuid: Option[String]): Option[Result] = acceptRequest(context, baseUuid)
  def acceptRequestWrite(context: RequestContext, baseUuid: Option[String]): Option[Result] = acceptRequest(context, baseUuid)
  def acceptRequestDelete(context: RequestContext, baseUuid: Option[String]): Option[Result] = acceptRequest(context, baseUuid)
}

trait AccessDecoratorControlForward extends AccessDecoratorControlMethods {
  val control: AccessDecoratorControl

  override def acceptRequest(context: RequestContext, baseUuid: Option[String]) = control.acceptRequest(context, baseUuid)
  override def acceptRequestRead(context: RequestContext, baseUuid: Option[String]) = control.acceptRequestRead(context, baseUuid)
  override def acceptRequestWrite(context: RequestContext, baseUuid: Option[String]) = control.acceptRequestWrite(context, baseUuid)
  override def acceptRequestDelete(context: RequestContext, baseUuid: Option[String]) = control.acceptRequestDelete(context, baseUuid)
}
