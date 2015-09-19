package modules.db.access

import controllers.api.nodes.RequestContext
import model.WustSchema._
import modules.db.Database._
import modules.db._
import play.api.libs.json.JsValue
import play.api.mvc.Results._
import play.api.mvc.Result
import renesca.graph.Label
import renesca.parameter.implicits._

trait NodeAccess[+NODE <: UuidNode] {
  val factory: UuidNodeMatchesFactory[NODE]

  def read(context: RequestContext): Either[Result, Iterable[NODE]]
  def read(context: RequestContext, uuid: String): Either[Result, NODE]
  def create(context: RequestContext): Either[Result, NODE]
  def update(context: RequestContext, uuid: String): Either[Result, NODE]
  def delete(context: RequestContext, uuid: String): Either[Result, Boolean]
}

trait NodeAccessDefault[NODE <: UuidNode] extends NodeAccess[NODE] {
  def read(context: RequestContext): Either[Result, Iterable[NODE]] = Left(NotFound("No read access on Node"))
  def read(context: RequestContext, uuid: String): Either[Result, NODE] = Left(NotFound("No read access on Node"))
  def create(context: RequestContext): Either[Result, NODE] = Left(NotFound("No create access on Node"))
  def update(context: RequestContext, uuid: String): Either[Result, NODE] = Left(NotFound("No update access on Node"))
  def delete(context: RequestContext, uuid: String): Either[Result, Boolean] = Left(NotFound("No delete access on Node"))
}

trait NodeReadBase[NODE <: UuidNode] extends NodeAccessDefault[NODE] {
  override def read(context: RequestContext) = {
    context.page.map { page =>
      val skip = page * context.limit
      Right(limitedDiscourseNodes(skip, context.limit, factory)._2)
    }.getOrElse(Right(discourseNodes(factory)._2))
  }

  override def read(context: RequestContext, uuid: String) = {
    val node = factory.matchesOnUuid(uuid)
    //TODO method for only resolving matches...
    db.transaction(_.persistChanges(node)) match {
      case Some(err) => Left(NotFound(s"Cannot find node with uuid '$uuid': $err"))
      case None => Right(node)
    }
  }
}

trait NodeDeleteBase[NODE <: UuidNode] extends NodeAccessDefault[NODE] {
  override def delete(context: RequestContext, uuid: String) = context.withUser {
    val node = factory.matchesOnUuid(uuid)
    // TODO: create Deleted action relation
    val failure = db.transaction(_.persistChanges(Discourse.remove(node)))
    Right(!failure.isDefined)
  }
}

case class NodeNothing[NODE <: UuidNode](factory: UuidNodeMatchesFactory[NODE]) extends NodeAccessDefault[NODE]
case class NodeRead[NODE <: UuidNode](factory: UuidNodeMatchesFactory[NODE]) extends NodeReadBase[NODE]
case class NodeDelete[NODE <: UuidNode](factory: UuidNodeMatchesFactory[NODE]) extends NodeDeleteBase[NODE]
case class NodeReadDelete[NODE <: UuidNode](factory: UuidNodeMatchesFactory[NODE]) extends NodeReadBase[NODE] with NodeDeleteBase[NODE]

trait NodeAccessDecorator[NODE <: UuidNode] extends NodeAccess[NODE] with AccessDecoratorControlDefault with AccessNodeDecoratorControlDefault[NODE] {
  val self: NodeAccess[NODE]

  override def read(context: RequestContext) = {
    acceptRequestRead(context).map(Left(_)).getOrElse(self.read(context).right.map(shapeResponse(_)))
  }
  override def read(context: RequestContext, uuid: String) = {
    acceptRequestRead(context).map(Left(_)).getOrElse(self.read(context, uuid).right.map(shapeResponse(_)))
  }
  override def create(context: RequestContext) = {
    acceptRequestWrite(context).map(Left(_)).getOrElse(self.create(context))
  }
  override def update(context: RequestContext, uuid: String) = {
    acceptRequestWrite(context).map(Left(_)).getOrElse(self.update(context, uuid))
  }
  override def delete(context: RequestContext, uuid: String) = {
    acceptRequestWrite(context).map(Left(_)).getOrElse(self.delete(context, uuid))
  }

  val factory = self.factory
}

case class NodeAccessDecoration[NODE <: UuidNode](self: NodeAccess[NODE], control: AccessDecoratorControl) extends NodeAccessDecorator[NODE] with AccessDecoratorControlForward
case class NodeAccessNodeDecoration[NODE <: UuidNode](self: NodeAccess[NODE], control: AccessNodeDecoratorControl[NODE]) extends NodeAccessDecorator[NODE] with AccessNodeDecoratorControlForward[NODE]
