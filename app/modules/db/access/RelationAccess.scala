package modules.db.access

import model.WustSchema.{UuidNode,User}
import modules.db.Database._
import modules.db._
import modules.db.types.UuidHyperNodeDefinitionBase
import modules.live.Broadcaster
import play.api.libs.json.JsValue
import renesca.schema._

trait RelationAccess[-NODE <: UuidNode,+OTHER <: UuidNode] {
  def read(baseDef: FixedNodeDefinition[NODE]): Either[Iterable[OTHER],String] = Right("No read access on Relation")
  def delete(baseDef: UuidNodeDefinition[NODE], uuid: String): Either[Boolean,String] = Right("No delete access on Relation")
  def deleteHyper(baseDef: UuidHyperNodeDefinitionBase[NODE with AbstractRelation[_,_]], uuid: String): Either[Boolean, String] = Right("No delete access on HyperRelation")
  def create(baseDef: UuidNodeDefinition[NODE], user: User, json: JsValue): Either[OTHER,String] = Right("No create access on Relation")
  def createHyper(baseDef: UuidHyperNodeDefinitionBase[NODE with AbstractRelation[_,_]], user: User, json: JsValue): Either[OTHER,String] = Right("No create access on HyperRelation")

  def toNodeDefinition: NodeDefinition[OTHER]
  def toNodeDefinition(uuid: String): UuidNodeDefinition[OTHER]

  def acceptsUpdateFrom(factory: AbstractRelationFactory[_,_,_], nodeFactory: Option[NodeFactory[_]]): Boolean
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
  // the same now holds for nodeschemas, as they also accepts AnyNodeSchemas, which does not implicate a concrete path.
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
] extends RelationAccess[START,END] with DirectedRelationAccess[START,RELATION,END]

trait EndRelationAccess[
  START <: UuidNode,
  RELATION <: AbstractRelation[START,END],
  END <: UuidNode
] extends RelationAccess[END,START] with DirectedRelationAccess[START,RELATION,END]

// TODO: AnyRelations should be deprecated or should be restrict to some
// labels...
@deprecated("Use strict RelationAccess", since = "renesca-0.3.0")
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

@deprecated("Use strict RelationAccess", since = "renesca-0.3.0")
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

trait StartRelationFactoryAccess[
START <: UuidNode,
RELATION <: AbstractRelation[START,END],
END <: UuidNode
] extends StartRelationAccess[START,RELATION,END] with RelationAndNodeUpdateAcceptor {
  val factory: AbstractRelationFactory[START, RELATION, END]
  val nodeFactory: NodeFactory[END]

  def toNodeDefinition = LabelNodeDefinition(nodeFactory)
  def toNodeDefinition(uuid: String) = FactoryUuidNodeDefinition(nodeFactory, uuid)
}

trait EndRelationFactoryAccess[
START <: UuidNode,
RELATION <: AbstractRelation[START,END],
END <: UuidNode
] extends EndRelationAccess[START,RELATION,END] with RelationAndNodeUpdateAcceptor {
  val factory: AbstractRelationFactory[START, RELATION, END]
  val nodeFactory: NodeFactory[START]

  def toNodeDefinition = LabelNodeDefinition(nodeFactory)
  def toNodeDefinition(uuid: String) = FactoryUuidNodeDefinition(nodeFactory, uuid)
}

class StartRelationRead[
  START <: UuidNode,
  RELATION <: AbstractRelation[START,END],
  END <: UuidNode
] (
  val factory: AbstractRelationFactory[START, RELATION, END],
  val nodeFactory: NodeFactory[END]
) extends StartRelationFactoryAccess[START,RELATION,END] with RelationAndNodeUpdateAcceptor {

  override def read(baseDef: FixedNodeDefinition[START]) = {
    Left(startConnectedDiscourseNodes(RelationDefinition(baseDef, factory, toNodeDefinition)))
  }
}

class EndRelationRead[
START <: UuidNode,
RELATION <: AbstractRelation[START,END],
END <: UuidNode
](
  val factory: AbstractRelationFactory[START, RELATION, END],
  val nodeFactory: NodeFactory[START]
  ) extends EndRelationFactoryAccess[START,RELATION,END] with RelationAndNodeUpdateAcceptor {

  override def read(baseDef: FixedNodeDefinition[END]) = {
    Left(endConnectedDiscourseNodes(RelationDefinition(toNodeDefinition, factory, baseDef)))
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

  override def delete(baseDef: UuidNodeDefinition[START], uuid:String) = {
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

class EndRelationReadDelete[
  START <: UuidNode,
  RELATION <: AbstractRelation[START,END],
  END <: UuidNode
 ](
   factory: AbstractRelationFactory[START, RELATION, END],
   nodeFactory: NodeFactory[START]
) extends EndRelationRead(factory, nodeFactory) {

  override def delete(baseDef: UuidNodeDefinition[END], uuid:String) = {
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
