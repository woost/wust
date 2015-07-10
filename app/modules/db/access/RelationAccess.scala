package modules.db.access

import model.WustSchema._
import modules.db.Database._
import modules.db._
import play.api.libs.json.JsValue
import renesca.schema._

trait RelationAccess[-NODE <: UuidNode, +OTHER <: UuidNode] {
  //TODO: switch left/right...left should be errors
  def read(baseDef: FixedNodeDefinition[NODE]): Either[Iterable[OTHER], String] = Right("No read access on Relation")
  def delete(baseDef: FixedNodeDefinition[NODE], uuid: String): Either[Boolean, String] = Right("No delete access on Relation")
  def deleteHyper(baseDef: HyperNodeDefinitionBase[NODE with AbstractRelation[_, _]], uuid: String): Either[Boolean, String] = Right("No delete access on HyperRelation")
  def create(uuid: String, user: User, json: JsValue): Either[OTHER, String]
  def create(uuid: String, user: User, otherUuid: String): Either[OTHER, String] = Right("No create access on Relation")
  def createHyper(startUuid: String, endUuid: String, user: User, json: JsValue): Either[OTHER, String]
  def createHyper(startUuid: String, endUuid: String, user: User, nestedUuid: String): Either[OTHER, String] = Right("No create access on HyperRelation")

  def toNodeDefinition: NodeDefinition[OTHER]
  def toNodeDefinition(uuid: String): UuidNodeDefinition[OTHER]
}

trait NodeAwareRelationAccess[NODE <: UuidNode, OTHER <: UuidNode] extends RelationAccess[NODE, OTHER] {
  private var nodeAccess: Option[NodeAccess[OTHER]] = None
  protected def withCreate(access: NodeAccess[OTHER]) = {
    nodeAccess = Some(access)
  }

  private def createNode(user: User, js: JsValue): Either[OTHER, String] = {
    nodeAccess.map(_.create(user, js)).getOrElse(Right("No factory defined on connect path"))
  }

  def create(uuid: String, user: User, json: JsValue): Either[OTHER, String] = {
    createNode(user, json).left.toOption.map(n => create(uuid, user, n.uuid)).getOrElse(Right("Cannot create node on connect path"))
  }
  def createHyper(startUuid: String, endUuid: String, user: User, json: JsValue): Either[OTHER, String] = {
    createNode(user, json).left.toOption.map(n => createHyper(startUuid, endUuid, user, n.uuid)).getOrElse(Right("Cannot create node on connect path"))
  }
}

trait DirectedRelationAccess[
START <: UuidNode,
RELATION <: AbstractRelation[START, END],
END <: UuidNode
] {
  val factory: AbstractRelationFactory[START, RELATION, END]
}

trait StartRelationAccess[
START <: UuidNode,
RELATION <: AbstractRelation[START, END],
END <: UuidNode
] extends NodeAwareRelationAccess[START,END] with DirectedRelationAccess[START, RELATION, END] {
  val nodeFactory: NodeFactory[END]

  def toNodeDefinition = ConcreteFactoryNodeDefinition(nodeFactory)
  def toNodeDefinition(uuid: String) = FactoryUuidNodeDefinition(nodeFactory, uuid)
}

trait EndRelationAccess[
START <: UuidNode,
RELATION <: AbstractRelation[START, END],
END <: UuidNode
] extends NodeAwareRelationAccess[END, START] with DirectedRelationAccess[START, RELATION, END] {
  val nodeFactory: NodeFactory[START]

  def toNodeDefinition = ConcreteFactoryNodeDefinition(nodeFactory)
  def toNodeDefinition(uuid: String) = FactoryUuidNodeDefinition(nodeFactory, uuid)
}

class StartRelationRead[
START <: UuidNode,
RELATION <: AbstractRelation[START, END],
END <: UuidNode
](
  val factory: AbstractRelationFactory[START, RELATION, END],
  val nodeFactory: NodeFactory[END]
  ) extends StartRelationAccess[START, RELATION, END] {

  override def read(baseDef: FixedNodeDefinition[START]) = {
    Left(startConnectedDiscourseNodes(RelationDefinition(baseDef, factory, toNodeDefinition)))
  }
}

object StartRelationRead {
  def apply[
  START <: UuidNode,
  RELATION <: AbstractRelation[START, END],
  END <: UuidNode
  ](
    factory: AbstractRelationFactory[START, RELATION, END],
    nodeFactory: NodeFactory[END]
    ): UuidNodeMatchesFactory[START] => StartRelationRead[START, RELATION, END] = {
    _ => new StartRelationRead(factory, nodeFactory)
  }
}

class EndRelationRead[
START <: UuidNode,
RELATION <: AbstractRelation[START, END],
END <: UuidNode
](
  val factory: AbstractRelationFactory[START, RELATION, END],
  val nodeFactory: NodeFactory[START]
  ) extends EndRelationAccess[START, RELATION, END] {

  override def read(baseDef: FixedNodeDefinition[END]) = {
    Left(endConnectedDiscourseNodes(RelationDefinition(toNodeDefinition, factory, baseDef)))
  }
}

object EndRelationRead {
  def apply[
  START <: UuidNode,
  RELATION <: AbstractRelation[START, END],
  END <: UuidNode
  ](
    factory: AbstractRelationFactory[START, RELATION, END],
    nodeFactory: NodeFactory[START]
    ): UuidNodeMatchesFactory[END] => EndRelationRead[START, RELATION, END] = {
    _ => new EndRelationRead(factory, nodeFactory)
  }
}

//TODO: correct results for delete....
class StartRelationReadDelete[
START <: UuidNode,
RELATION <: AbstractRelation[START, END],
END <: UuidNode
](
  factory: AbstractRelationFactory[START, RELATION, END],
  nodeFactory: NodeFactory[END]
  ) extends StartRelationRead(factory, nodeFactory) {

  override def delete(baseDef: FixedNodeDefinition[START], uuid: String) = {
    val relationDefinition = RelationDefinition(baseDef, factory, toNodeDefinition(uuid))
    disconnectNodes(relationDefinition)
    Left(true)
  }

  override def deleteHyper(baseDef: HyperNodeDefinitionBase[START with AbstractRelation[_, _]], uuid: String) = {
    val relationDefinition = RelationDefinition(baseDef, factory, toNodeDefinition(uuid))
    disconnectNodes(relationDefinition)
    Left(true)
  }
}

object StartRelationReadDelete {
  def apply[
  START <: UuidNode,
  RELATION <: AbstractRelation[START, END],
  END <: UuidNode
  ](
    factory: AbstractRelationFactory[START, RELATION, END],
    nodeFactory: NodeFactory[END]
    ): UuidNodeDefinition[START] => StartRelationReadDelete[START, RELATION, END] = {
    _ => new StartRelationReadDelete(factory, nodeFactory)
  }
}

class EndRelationReadDelete[
START <: UuidNode,
RELATION <: AbstractRelation[START, END],
END <: UuidNode
](
  factory: AbstractRelationFactory[START, RELATION, END],
  nodeFactory: NodeFactory[START]
  ) extends EndRelationRead(factory, nodeFactory) {

  override def delete(baseDef: FixedNodeDefinition[END], uuid: String) = {
    val relationDefinition = RelationDefinition(toNodeDefinition(uuid), factory, baseDef)
    disconnectNodes(relationDefinition)
    Left(true)
  }

  override def deleteHyper(baseDef: HyperNodeDefinitionBase[END with AbstractRelation[_, _]], uuid: String) = {
    val relationDefinition = RelationDefinition(toNodeDefinition(uuid), factory, baseDef)
    disconnectNodes(relationDefinition)
    Left(true)
  }
}

object EndRelationReadDelete {
  def apply[
  START <: UuidNode,
  RELATION <: AbstractRelation[START, END],
  END <: UuidNode
  ](
    factory: AbstractRelationFactory[START, RELATION, END],
    nodeFactory: NodeFactory[START]
    ): UuidNodeMatchesFactory[END] => EndRelationReadDelete[START, RELATION, END] = {
    _ => new EndRelationReadDelete(factory, nodeFactory)
  }
}
