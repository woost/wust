package modules.db.access

import controllers.api.nodes.RequestContext
import play.api.mvc.Result

trait AccessDecoratorControlMethods {
  def acceptRequest(context: RequestContext): Option[Result]
}

trait AccessDecoratorControl extends AccessDecoratorControlMethods {
}

trait AccessDecoratorControlDefault extends AccessDecoratorControl {
  override def acceptRequest(context: RequestContext): Option[Result] = None
}
