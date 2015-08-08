package modules.db.access

import controllers.api.nodes.RequestContext

trait AccessDecoratorControl {
  def acceptRequest(context: RequestContext): Option[String]
}

trait AccessDecoratorControlDefault extends AccessDecoratorControl {
  override def acceptRequest(context: RequestContext): Option[String] = None
}
