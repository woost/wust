package modules.db.access

import model.WustSchema._
import modules.db.Database._
import modules.db._
import modules.live.Broadcaster
import play.api.libs.json.JsValue
import renesca.schema._

trait NodeAccess[NODE <: UuidNode] {
  val factory: NodeFactory[NODE]
  val name = factory.getClass.getSimpleName.dropRight(1)

  def read: Either[Iterable[NODE],String] = Right("No read access on Node collection")
  def read(uuid: String): Either[NODE, String] = Right("No read access on Node")
  def create(user: User, json: JsValue): Either[NODE, String] = Right("No create access on Node")
  def update(uuid: String, user: User, nodeAdd: JsValue): Either[NODE,String] = Right("No update access on Node")
  def delete(uuid: String): Either[Boolean,String] = Right("No delete access on Node")
}

class NodeRead[NODE <: UuidNode](val factory: NodeFactory[NODE]) extends NodeAccess[NODE] {
  override def read = {
    Left(discourseNodes(factory)._2.toSet)
  }

  override def read(uuid: String) = {
    discourseNodes(factory, uuid)._2.headOption match {
      case Some(node) => Left(node)
      case None       => Right(s"Cannot find node with uuid '$uuid' and label '${factory.label}'")
    }
  }
}

class NodeReadDelete[NODE <: UuidNode](factory: NodeFactory[NODE]) extends NodeRead(factory) {
  override def delete(uuid: String) = {
    deleteNodes(FactoryUuidNodeDefinition(factory, uuid))
    Broadcaster.broadcastDelete(factory, uuid)
    Left(true)
  }
}
