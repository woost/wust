package modules.db.access

import controllers.api.nodes.RequestContext
import play.api.mvc.Result
import model.WustSchema.UuidNode

//TODO: rename to before_action
//TODO: maybe have just one accessdecoratorcontrol
//but we would need a generic here then, but that should work.
trait AccessDecoratorControlMethods {
  def acceptRequest(context: RequestContext): Option[Result]
  def acceptRequestRead(context: RequestContext): Option[Result]
  def acceptRequestWrite(context: RequestContext): Option[Result]
  def acceptRequestDelete(context: RequestContext): Option[Result]
}

trait AccessNodeDecoratorControlMethods[NODE <: UuidNode] {
  def shapeResponse(response: NODE): NODE
  def shapeResponse(response: Iterable[NODE]): Iterable[NODE]
}

trait AccessDecoratorControl extends AccessDecoratorControlMethods
trait AccessNodeDecoratorControl[NODE <: UuidNode] extends AccessNodeDecoratorControlMethods[NODE]

trait AccessDecoratorControlDefault extends AccessDecoratorControlMethods {
  def acceptRequest(context: RequestContext): Option[Result] = None
  def acceptRequestRead(context: RequestContext): Option[Result] = acceptRequest(context)
  def acceptRequestWrite(context: RequestContext): Option[Result] = acceptRequest(context)
  def acceptRequestDelete(context: RequestContext): Option[Result] = acceptRequest(context)
}

trait AccessNodeDecoratorControlDefault[NODE <: UuidNode] extends AccessNodeDecoratorControlMethods[NODE] {
  def shapeResponse(response: NODE): NODE = response
  def shapeResponse(response: Iterable[NODE]): Iterable[NODE] = response
}

trait AccessDecoratorControlForward extends AccessDecoratorControlMethods {
  val control: AccessDecoratorControl

  override def acceptRequest(context: RequestContext) = control.acceptRequest(context)
  override def acceptRequestRead(context: RequestContext) = control.acceptRequestRead(context)
  override def acceptRequestWrite(context: RequestContext) = control.acceptRequestWrite(context)
  override def acceptRequestDelete(context: RequestContext) = control.acceptRequestDelete(context)
}

trait AccessNodeDecoratorControlForward[NODE <: UuidNode] extends AccessNodeDecoratorControlMethods[NODE] {
  val control: AccessNodeDecoratorControl[NODE]

  override def shapeResponse(response: NODE) = control.shapeResponse(response)
  override def shapeResponse(response: Iterable[NODE]) = control.shapeResponse(response)
}
