package modules.db.access

import controllers.api.nodes.RequestContext

trait AccessDecoratorControlMethods {
  def acceptRequest(context: RequestContext): Option[String]
}

trait AccessDecoratorControl extends AccessDecoratorControlMethods {
}

trait AccessDecoratorControlDefault extends AccessDecoratorControl {
  override def acceptRequest(context: RequestContext): Option[String] = None
}
