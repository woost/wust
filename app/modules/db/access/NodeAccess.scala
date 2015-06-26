package modules.db.access

import model.WustSchema._
import modules.db.Database._
import modules.db._
import modules.live.Broadcaster
import play.api.libs.json.JsValue
import renesca.graph.Label
import renesca.schema._

trait NodeAccess[NODE <: UuidNode] {
  val name: String
  val label: Option[Label]

  def acceptsUpdateFrom(factory: NodeFactory[_]): Boolean

  def read: Either[Iterable[NODE],String] = Right("No read access on Node collection")
  def read(uuid: String): Either[NODE, String] = Right("No read access on Node")
  def create(user: User, json: JsValue): Either[NODE, String] = Right("No create access on Node")
  def update(uuid: String, user: User, nodeAdd: JsValue): Either[NODE,String] = Right("No update access on Node")
  def delete(uuid: String): Either[Boolean,String] = Right("No delete access on Node")

  def toNodeDefinition(uuid: String): UuidNodeDefinition[NODE]
}

@deprecated("Use strict NodeAccess", since = "renesca-0.3.0")
class AnyContentNode() extends NodeAccess[ContentNode] {
  val name = "ContentNode"
  val label = None

  def acceptsUpdateFrom(factory: NodeFactory[_]) = true

  def toNodeDefinition = AnyNodeDefinition()
  def toNodeDefinition(uuid: String) = AnyUuidNodeDefinition(uuid)

  override def read = {
    Left(discourseGraph(toNodeDefinition).contentNodes)
  }

  override def read(uuid: String) = {
    discourseGraph(toNodeDefinition(uuid)).contentNodes.headOption match {
      case Some(node) => Left(node)
      case None       => Right(s"Cannot find node with uuid '$uuid'")
    }
  }
}

trait NodeAccessWithFactory[NODE <: UuidNode] extends NodeAccess[NODE] {
  val factory: UuidNodeFactory[NODE]
  val name = factory.getClass.getSimpleName.dropRight(1)
  val label = Some(factory.label)

  def acceptsUpdateFrom(factory: NodeFactory[_]) = this.factory == factory

  def toNodeDefinition(uuid: String) = FactoryUuidNodeDefinition(factory, uuid)
}

// TODO: use Node.matches, but we do not know the signature of the factory methods here...
class NodeRead[NODE <: UuidNode](val factory: UuidNodeFactory[NODE]) extends NodeAccessWithFactory[NODE] {
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

class NodeReadDelete[NODE <: UuidNode](factory: UuidNodeFactory[NODE]) extends NodeRead(factory) {
  override def delete(uuid: String) = {
    deleteNodes(FactoryUuidNodeDefinition(factory, uuid))
    // TODO: use matches...
    // TODO: create Deleted action relation
    //Broadcaster.broadcastConnect(user, RelationDefinition(ConcreteFactoryNodeDefinition(User), Deleted, ConcreteFactoryNodeDefinition(factory)), node)
    Broadcaster.broadcastDelete(factory, uuid)
    Left(true)
  }
}
