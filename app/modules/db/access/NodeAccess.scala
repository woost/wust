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

  def read(context: RequestContext): Either[Iterable[NODE],String] = Right("No read access on Node collection")
  def read(context: RequestContext, uuid: String): Either[NODE, String] = Right("No read access on Node")
  def create(context: RequestContext): Either[NODE, String] = Right("No create access on Node")
  def update(context: RequestContext, uuid: String): Either[NODE,String] = Right("No update access on Node")
  def delete(context: RequestContext, uuid: String): Either[Boolean,String] = Right("No delete access on Node")

  def toNodeDefinition(uuid: String) = FactoryUuidNodeDefinition(factory, uuid)
}

class NodeRead[NODE <: UuidNode](val factory: UuidNodeMatchesFactory[NODE]) extends NodeAccess[NODE] {
  override def read(context: RequestContext) = {
    context.page.map { page =>
      val skip = page * context.limit
      Left(limitedDiscourseNodes(skip, context.limit, factory)._2)
    }.getOrElse(Left(discourseNodes(factory)._2))
  }

  override def read(context: RequestContext, uuid: String) = {
    //TODO possibility to resolve nodes directly without graph?
    val discourse = Discourse.empty
    val node = factory.matchesOnUuid(uuid)
    discourse.add(node)
    //TODO method for only resolving matches...
    db.transaction(_.persistChanges(discourse)) match {
      case Some(err) => Right(s"Cannot find node with uuid '$uuid'': $err")
      case None => Left(node)
    }
  }
}

object NodeRead {
  def apply[NODE <: UuidNode](factory: UuidNodeMatchesFactory[NODE]) = new NodeRead(factory)
}

class NodeReadDelete[NODE <: UuidNode](factory: UuidNodeMatchesFactory[NODE]) extends NodeRead(factory) {
  override def delete(context: RequestContext, uuid: String) = {
    // TODO: use matches... and remove...
    deleteNodes(FactoryUuidNodeDefinition(factory, uuid))
    // TODO: create Deleted action relation
    Left(true)
  }
}

object NodeReadDelete {
  def apply[NODE <: UuidNode](factory: UuidNodeMatchesFactory[NODE]) = new NodeReadDelete(factory)
}
