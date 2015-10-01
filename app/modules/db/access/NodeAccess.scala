package modules.db.access

import controllers.api.nodes.RequestContext
import model.WustSchema._
import modules.db.Database._
import play.api.libs.json._
import play.api.mvc.Result
import play.api.mvc.Results._

trait NodeAccess[+NODE <: UuidNode] {
  val factory: UuidNodeMatchesFactory[NODE]

  def read(context: RequestContext): Result
  def read(context: RequestContext, uuid: String): Result
  def create(context: RequestContext): Result
  def update(context: RequestContext, uuid: String): Result
  def delete(context: RequestContext, uuid: String): Result
}

trait NodeAccessDefault[NODE <: UuidNode] extends NodeAccess[NODE] {
  def read(context: RequestContext) = NotFound("No read access on Node")
  def read(context: RequestContext, uuid: String) = NotFound("No read access on Node")
  def create(context: RequestContext) = NotFound("No create access on Node")
  def update(context: RequestContext, uuid: String) = NotFound("No update access on Node")
  def delete(context: RequestContext, uuid: String) = NotFound("No delete access on Node")
}

trait FormattingNode[NODE <: UuidNode] {
  implicit def format: Format[NODE]
}

trait NodeReadBase[NODE <: UuidNode] extends NodeAccessDefault[NODE] with FormattingNode[NODE] {
  override def read(context: RequestContext) = {
    context.page.map { page =>
      val skip = page * context.limit
      Ok(Json.toJson(limitedDiscourseNodes(skip, context.limit, factory)._2))
    }.getOrElse(Ok(Json.toJson(discourseNodes(factory)._2)))
  }

  override def read(context: RequestContext, uuid: String) = {
    val node = factory.matchesOnUuid(uuid)
    db.transaction(_.persistChanges(node)) match {
      case Some(err) => NotFound(s"Cannot find node with uuid '$uuid': $err")
      case None      => Ok(Json.toJson(node))
    }
  }
}

trait NodeDeleteBase[NODE <: UuidNode] extends NodeAccessDefault[NODE] with FormattingNode[NODE] {
  override def delete(context: RequestContext, uuid: String) = context.withUser {
    val node = factory.matchesOnUuid(uuid)
    val failure = db.transaction(_.persistChanges(Discourse.remove(node)))
    if(failure.isDefined)
      BadRequest("Cannot delete node")
    else
      NoContent
  }
}

case class NodeNothing[NODE <: UuidNode](factory: UuidNodeMatchesFactory[NODE]) extends NodeAccessDefault[NODE]
case class NodeRead[NODE <: UuidNode](factory: UuidNodeMatchesFactory[NODE])(implicit val format: Format[NODE]) extends NodeReadBase[NODE]
case class NodeDelete[NODE <: UuidNode](factory: UuidNodeMatchesFactory[NODE])(implicit val format: Format[NODE]) extends NodeDeleteBase[NODE]
case class NodeReadDelete[NODE <: UuidNode](factory: UuidNodeMatchesFactory[NODE])(implicit val format: Format[NODE]) extends NodeReadBase[NODE] with NodeDeleteBase[NODE]

trait NodeAccessDecorator[NODE <: UuidNode] extends NodeAccess[NODE] with AccessDecoratorControlDefault {
  val self: NodeAccess[NODE]

  override def read(context: RequestContext) = {
    acceptRequestRead(context).getOrElse(self.read(context))
  }
  override def read(context: RequestContext, uuid: String) = {
    acceptRequestRead(context).getOrElse(self.read(context, uuid))
  }
  override def create(context: RequestContext) = {
    acceptRequestWrite(context).getOrElse(self.create(context))
  }
  override def update(context: RequestContext, uuid: String) = {
    acceptRequestWrite(context).getOrElse(self.update(context, uuid))
  }
  override def delete(context: RequestContext, uuid: String) = {
    acceptRequestWrite(context).getOrElse(self.delete(context, uuid))
  }

  val factory = self.factory
}

case class NodeAccessDecoration[NODE <: UuidNode](self: NodeAccess[NODE], control: AccessDecoratorControl) extends NodeAccessDecorator[NODE] with AccessDecoratorControlForward
