package modules.db.access

import controllers.api.nodes.RequestContext
import model.WustSchema._
import modules.db.Database._
import modules.db._
import play.api.libs.json.JsValue
import renesca.graph.Label
import renesca.parameter.implicits._

trait NodeAccess[+NODE <: UuidNode] {
  val factory: UuidNodeMatchesFactory[NODE]
  val name = factory.getClass.getSimpleName.dropRight(1)
  val label = factory.label

  def read(context: RequestContext): Either[String, Iterable[NODE]] = Left("No read access on Node collection")
  def read(context: RequestContext, uuid: String): Either[String, NODE] = Left("No read access on Node")
  def create(context: RequestContext): Either[String, NODE] = Left("No create access on Node")
  def update(context: RequestContext, uuid: String): Either[String, NODE] = Left("No update access on Node")
  def delete(context: RequestContext, uuid: String): Either[String, Boolean] = Left("No delete access on Node")
}

trait NodeReadBase[NODE <: UuidNode] extends NodeAccess[NODE] {
  override def read(context: RequestContext) = {
    context.page.map { page =>
      val skip = page * context.limit
      Right(limitedDiscourseNodes(skip, context.limit, factory)._2)
    }.getOrElse(Right(discourseNodes(factory)._2))
  }

  override def read(context: RequestContext, uuid: String) = {
    //TODO possibility to resolve nodes directly without graph?
    val node = factory.matchesOnUuid(uuid)
    //TODO method for only resolving matches...
    db.transaction(_.persistChanges(node)) match {
      case Some(err) => Left(s"Cannot find node with uuid '$uuid'': $err")
      case None =>
        Right(node)
    }
  }
}

trait NodeDeleteBase[NODE <: UuidNode] extends NodeAccess[NODE] {
  override def delete(context: RequestContext, uuid: String) = {
    // TODO: use matches... and remove...
    deleteNodes(FactoryUuidNodeDefinition(factory, uuid))
    // TODO: create Deleted action relation
    Right(true)
  }
}

case class NodeRead[NODE <: UuidNode](factory: UuidNodeMatchesFactory[NODE]) extends NodeReadBase[NODE]
case class NodeDelete[NODE <: UuidNode](factory: UuidNodeMatchesFactory[NODE]) extends NodeDeleteBase[NODE]
case class NodeReadDelete[NODE <: UuidNode](factory: UuidNodeMatchesFactory[NODE]) extends NodeReadBase[NODE] with NodeDeleteBase[NODE]
