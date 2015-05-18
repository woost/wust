package modules.db.access

import model.WustSchema.UuidNode
import modules.db.Database._
import modules.db._
import modules.db.types.UuidHyperNodeDefinitionBase
import modules.live.Broadcaster
import play.api.libs.json.JsValue
import renesca.schema._

trait RelationAccess[NODE <: UuidNode] {
  def read(baseDef: FixedNodeDefinition[NODE]): Either[Set[_ <: UuidNode],String]
  def delete(baseDef: FactoryUuidNodeDefinition[NODE], uuid: String): Either[Boolean,String] = Right("No delete access on Relation")
  def deleteHyper(baseDef: UuidHyperNodeDefinitionBase[NODE with AbstractRelation[_,_]], uuid: String): Either[Boolean, String] = Right("No delete access on HyperRelation")
  def create(baseDef: FactoryUuidNodeDefinition[NODE], json: JsValue): Either[_ <: UuidNode,String]
  def createHyper(baseDef: UuidHyperNodeDefinitionBase[NODE with AbstractRelation[_,_]], json: JsValue): Either[_ <: UuidNode,String]

  def acceptsUpdateFrom(factory: AbstractRelationFactory[_,_,_], nodeFactory: Option[NodeFactory[_]]): Boolean
}

trait OppositeRelationAccess[NODE <: UuidNode] {
  def toNodeDefinition: NodeDefinition[NODE]
  def toNodeDefinition(uuid: String): UuidNodeDefinition[NODE]
}

trait CombinedRelationAccess[BASE <: UuidNode, OTHER <: UuidNode] extends RelationAccess[BASE] with OppositeRelationAccess[OTHER] {
  def read(baseDef: FixedNodeDefinition[BASE]): Either[Set[OTHER],String] = Right("No read access on Relation")
  def create(baseDef: FactoryUuidNodeDefinition[BASE], json: JsValue): Either[OTHER,String] = Right("No create access on Relation")
  def createHyper(baseDef: UuidHyperNodeDefinitionBase[BASE with AbstractRelation[_,_]], json: JsValue): Either[OTHER,String] = Right("No create access on HyperRelation")
}

trait RelationUpdateAcceptor {
  val factory: AbstractRelationFactory[_,_,_]
  def acceptsUpdateFrom(factory: AbstractRelationFactory[_,_,_], nodeFactory: Option[NodeFactory[_]]): Boolean = {
    this.factory == factory
  }
}

trait RelationAndNodeUpdateAcceptor {
  val factory: AbstractRelationFactory[_,_,_]
  val nodeFactory: NodeFactory[_]

  //TODO: what about a deletable/writable Relation beneath a HyperConnectSchema with AnyRelationAccess:
  // writing/deleting on the nested relation is allowed and therefore events could be generated.
  // for AnyRelationAccess, the optional nodefactory will be none. If we accept such updates, we might get updates
  // for nodes we are not interested in, if we decline, we might miss some updates regarding our own kind of node.
  // It is probably not desirable to POST/DELETE on something like: problems/:id/ANY/:id/pros
  // It is already impossible to write/delete on ConnectSchemas with AnyRelationAccess, maybe also forbid writable child
  // connectschemas within HyperConnectSchema with AnyRelationAccess? regard: other than for events, this is well-defined.
  def acceptsUpdateFrom(factory: AbstractRelationFactory[_,_,_], nodeFactory: Option[NodeFactory[_]]): Boolean = {
    this.factory == factory && (nodeFactory.isDefined && this.nodeFactory == nodeFactory.get)
  }
}

trait DirectedRelationAccess[
  START <: UuidNode,
  RELATION <: AbstractRelation[START,END],
  END <: UuidNode
] {
  val factory: AbstractRelationFactory[START,RELATION,END]
}

trait StartRelationAccess[
  START <: UuidNode,
  RELATION <: AbstractRelation[START,END],
  END <: UuidNode
] extends CombinedRelationAccess[START,END] with DirectedRelationAccess[START,RELATION,END]

trait EndRelationAccess[
  START <: UuidNode,
  RELATION <: AbstractRelation[START,END],
  END <: UuidNode
] extends CombinedRelationAccess[END,START] with DirectedRelationAccess[START,RELATION,END]

class StartAnyRelation[
  START <: UuidNode,
  RELATION <: AbstractRelation[START,END],
  END <: UuidNode
](
  val factory: AbstractRelationFactory[START, RELATION, END]
) extends StartRelationAccess[START,RELATION,END] with RelationUpdateAcceptor {

  def toNodeDefinition = AnyNodeDefinition()
  def toNodeDefinition(uuid: String) = AnyUuidNodeDefinition(uuid)

  override def read(baseDef: FixedNodeDefinition[START]) = {
    Left(startConnectedDiscourseNodes(RelationDefinition(baseDef, factory, toNodeDefinition)))
  }
}

class EndAnyRelation[
START <: UuidNode,
RELATION <: AbstractRelation[START,END],
END <: UuidNode
](
  val factory: AbstractRelationFactory[START, RELATION, END]
) extends EndRelationAccess[START,RELATION,END] with RelationUpdateAcceptor {

  def toNodeDefinition = AnyNodeDefinition()
  def toNodeDefinition(uuid: String) = AnyUuidNodeDefinition(uuid)

  override def read(baseDef: FixedNodeDefinition[END]) = {
    Left(endConnectedDiscourseNodes(RelationDefinition(toNodeDefinition, factory, baseDef)))
  }
}

class StartRelationRead[
  START <: UuidNode,
  RELATION <: AbstractRelation[START,END],
  END <: UuidNode
] (
  val factory: AbstractRelationFactory[START, RELATION, END],
  val nodeFactory: NodeFactory[END]
) extends StartRelationAccess[START,RELATION,END] with RelationAndNodeUpdateAcceptor {

  def toNodeDefinition = LabelNodeDefinition(nodeFactory)
  def toNodeDefinition(uuid: String) = FactoryUuidNodeDefinition(nodeFactory, uuid)

  override def read(baseDef: FixedNodeDefinition[START]) = {
    Left(startConnectedDiscourseNodes(RelationDefinition(baseDef, factory, toNodeDefinition)))
  }
}

class StartRelationReadDelete[
  START <: UuidNode,
  RELATION <: AbstractRelation[START,END],
  END <: UuidNode
](
  factory: AbstractRelationFactory[START, RELATION, END],
  nodeFactory: NodeFactory[END]
) extends StartRelationRead(factory, nodeFactory) {

  override def delete(baseDef: FactoryUuidNodeDefinition[START], uuid:String) = {
    val relationDefinition = RelationDefinition(baseDef, factory, toNodeDefinition(uuid))
    disconnectNodes(relationDefinition)
    Broadcaster.broadcastDisconnect(relationDefinition)
    Left(true)
  }

  override def deleteHyper(baseDef: UuidHyperNodeDefinitionBase[START with AbstractRelation[_,_]], uuid: String) = {
    val relationDefinition = RelationDefinition(baseDef, factory, toNodeDefinition(uuid))
    disconnectNodes(relationDefinition)
    Broadcaster.broadcastStartHyperDisconnect(relationDefinition)
    Left(true)
  }
}

class EndRelationRead[
  START <: UuidNode,
  RELATION <: AbstractRelation[START,END],
  END <: UuidNode
](
  val factory: AbstractRelationFactory[START, RELATION, END],
  val nodeFactory: NodeFactory[START]
) extends EndRelationAccess[START,RELATION,END] with RelationAndNodeUpdateAcceptor {

  def toNodeDefinition = LabelNodeDefinition(nodeFactory)
  def toNodeDefinition(uuid: String) = FactoryUuidNodeDefinition(nodeFactory, uuid)

  override def read(baseDef: FixedNodeDefinition[END]) = {
    Left(endConnectedDiscourseNodes(RelationDefinition(toNodeDefinition, factory, baseDef)))
  }
}

class EndRelationReadDelete[
  START <: UuidNode,
  RELATION <: AbstractRelation[START,END],
  END <: UuidNode
 ](
   factory: AbstractRelationFactory[START, RELATION, END],
   nodeFactory: NodeFactory[START]
) extends EndRelationRead(factory, nodeFactory) {

  override def delete(baseDef: FactoryUuidNodeDefinition[END], uuid:String) = {
    val relationDefinition = RelationDefinition(toNodeDefinition(uuid), factory, baseDef)
    disconnectNodes(relationDefinition)
    Broadcaster.broadcastDisconnect(relationDefinition)
    Left(true)
  }

  override def deleteHyper(baseDef: UuidHyperNodeDefinitionBase[END with AbstractRelation[_,_]], uuid: String) = {
    val relationDefinition = RelationDefinition(toNodeDefinition(uuid), factory, baseDef)
    disconnectNodes(relationDefinition)
    Broadcaster.broadcastEndHyperDisconnect(relationDefinition)
    Left(true)
  }
}
