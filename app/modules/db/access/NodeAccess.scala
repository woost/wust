package modules.db.access

import model.WustSchema._
import modules.db.Database._
import modules.db._
import play.api.libs.json.JsValue
import renesca.graph.Label
import renesca.parameter.implicits._

trait NodeAccess[+NODE <: UuidNode] {
  val name: String
  // TODO: should not be an option -> see deprecated AnyContentNode
  val label: Option[Label]

  def read: Either[Iterable[NODE],String] = Right("No read access on Node collection")
  def read(uuid: String): Either[NODE, String] = Right("No read access on Node")
  def create(user: User, json: JsValue): Either[NODE, String] = Right("No create access on Node")
  def update(uuid: String, user: User, nodeAdd: JsValue): Either[NODE,String] = Right("No update access on Node")
  def delete(uuid: String): Either[Boolean,String] = Right("No delete access on Node")

  def toNodeDefinition(uuid: String): UuidNodeDefinition[NODE]
}

trait FactoryNodeAccess[NODE <: UuidNode] extends NodeAccess[NODE] {
  val factory: UuidNodeFactory[NODE]
  val name = factory.getClass.getSimpleName.dropRight(1)
  val label = Some(factory.label)

  def toNodeDefinition(uuid: String) = FactoryUuidNodeDefinition(factory, uuid)
}

@deprecated("Use LabelNodeRead(name: String, label: Label)", since = "renesca-0.3.0")
class AnyContentNode() extends NodeAccess[ContentNode] {
  val name = "ContentNode"
  val label = None

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

object AnyContentNode {
  def apply = new AnyContentNode
}

class LabelNodeRead(val name: String, _label: Label) extends NodeAccess[UuidNode] {
  val label = Some(_label)

  def toNodeDefinition = LabelNodeDefinition(_label)
  def toNodeDefinition(uuid: String) = LabelUuidNodeDefinition(_label, uuid)

  override def read = {
    Left(discourseGraph(toNodeDefinition).uuidNodes)
  }

  override def read(uuid: String) = {
    discourseGraph(toNodeDefinition(uuid)).contentNodes.headOption match {
      case Some(node) => Left(node)
      case None       => Right(s"Cannot find node with uuid '$uuid'")
    }
  }
}

object LabelNodeRead {
  def apply(name: String, label: Label) = new LabelNodeRead(name, label)
}

class NodeRead[NODE <: UuidNode](val factory: UuidNodeFactory[NODE]) extends FactoryNodeAccess[NODE] {
  override def read = {
    Left(discourseNodes(factory)._2.toSet)
  }

  override def read(uuid: String) = {
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
  def apply[NODE <: UuidNode](factory: UuidNodeFactory[NODE]) = new NodeRead(factory)
}

class NodeReadDelete[NODE <: UuidNode](factory: UuidNodeFactory[NODE]) extends NodeRead(factory) {
  override def delete(uuid: String) = {
    // TODO: use matches... and remove...
    deleteNodes(FactoryUuidNodeDefinition(factory, uuid))
    // TODO: create Deleted action relation
    Left(true)
  }
}

object NodeReadDelete {
  def apply[NODE <: UuidNode](factory: UuidNodeFactory[NODE]) = new NodeReadDelete(factory)
}
