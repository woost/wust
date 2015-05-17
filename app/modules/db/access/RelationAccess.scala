package modules.db.access

import model.WustSchema._
import modules.db.Database._
import modules.db._
import modules.db.types.UuidHyperNodeDefinitionBase
import modules.live.Broadcaster
import play.api.libs.json.JsValue
import renesca.schema._

trait RelationAccess[NODE <: UuidNode] {
  def read(baseDef: FixedNodeDefinition[NODE]): Either[Set[_ <: UuidNode],String]
  def delete(baseDef: UuidNodeDefinition[NODE], uuid: String): Either[Boolean,String] = Right("No delete access on Relation")
  def deleteHyper(baseDef: UuidHyperNodeDefinitionBase[NODE with AbstractRelation[_,_]], uuid: String): Either[Boolean, String] = Right("No delete access on HyperRelation")
  def create(baseDef: UuidNodeDefinition[NODE], json: JsValue): Either[_ <: UuidNode,String]
  def createHyper(baseDef: UuidHyperNodeDefinitionBase[NODE with AbstractRelation[_,_]], json: JsValue): Either[_ <: UuidNode,String]
}

trait OppositeRelationAccess[NODE <: UuidNode] {
  def toNodeDefinition: NodeDefinition[NODE]
  def toNodeDefinition(uuid: String): UuidNodeDefinition[NODE]
}

trait CombinedRelationAccess[BASE <: UuidNode, OTHER <: UuidNode] extends RelationAccess[BASE] with OppositeRelationAccess[OTHER] {
  def read(baseDef: FixedNodeDefinition[BASE]): Either[Set[OTHER],String] = Right("No read access on Relation")
  def create(baseDef: UuidNodeDefinition[BASE], json: JsValue): Either[OTHER,String] = Right("No create access on Relation")
  def createHyper(baseDef: UuidHyperNodeDefinitionBase[BASE with AbstractRelation[_,_]], json: JsValue): Either[OTHER,String] = Right("No create access on HyperRelation")
}

trait StartRelationAccess[
  START <: UuidNode,
  RELATION <: AbstractRelation[START,END],
  END <: UuidNode
] extends CombinedRelationAccess[START,END] {
  val factory: AbstractRelationFactory[START, RELATION, END]
}

trait EndRelationAccess[
  START <: UuidNode,
  RELATION <: AbstractRelation[START,END],
  END <: UuidNode
] extends CombinedRelationAccess[END,START] {
  val factory: AbstractRelationFactory[START, RELATION, END]
}

class StartAnyRelation[
  START <: UuidNode,
  RELATION <: AbstractRelation[START,END],
  END <: UuidNode
] (
  val factory: AbstractRelationFactory[START, RELATION, END]
) extends StartRelationAccess[START,RELATION,END] {

  def toNodeDefinition = AnyNodeDefinition()
  def toNodeDefinition(uuid: String) = AnyUuidNodeDefinition(uuid)

  override def read(baseDef: FixedNodeDefinition[START]) = {
    Left(startConnectedDiscourseNodes(RelationDefinition(baseDef, factory, toNodeDefinition))._2)
  }
}

class EndAnyRelation[
START <: UuidNode,
RELATION <: AbstractRelation[START,END],
END <: UuidNode
] (
  val factory: AbstractRelationFactory[START, RELATION, END]
  ) extends EndRelationAccess[START,RELATION,END] {

  def toNodeDefinition = AnyNodeDefinition()
  def toNodeDefinition(uuid: String) = AnyUuidNodeDefinition(uuid)

  override def read(baseDef: FixedNodeDefinition[END]) = {
    Left(endConnectedDiscourseNodes(RelationDefinition(toNodeDefinition, factory, baseDef))._2)
  }
}

class StartRelationRead[
  START <: UuidNode,
  RELATION <: AbstractRelation[START,END],
  END <: UuidNode
] (
  val factory: AbstractRelationFactory[START, RELATION, END],
  val nodeFactory: NodeFactory[END]
) extends StartRelationAccess[START,RELATION,END] {

  def toNodeDefinition = LabelNodeDefinition(nodeFactory)
  def toNodeDefinition(uuid: String) = FactoryUuidNodeDefinition(nodeFactory, uuid)

  override def read(baseDef: FixedNodeDefinition[START]) = {
    Left(startConnectedDiscourseNodes(RelationDefinition(baseDef, factory, toNodeDefinition))._2)
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

class EndRelationRead[
  START <: UuidNode,
  RELATION <: AbstractRelation[START,END],
  END <: UuidNode
](
  val factory: AbstractRelationFactory[START, RELATION, END],
  val nodeFactory: NodeFactory[START]
) extends EndRelationAccess[START,RELATION,END] {

  def toNodeDefinition = LabelNodeDefinition(nodeFactory)
  def toNodeDefinition(uuid: String) = FactoryUuidNodeDefinition(nodeFactory, uuid)

  override def read(baseDef: FixedNodeDefinition[END]) = {
    Left(endConnectedDiscourseNodes(RelationDefinition(toNodeDefinition, factory, baseDef))._2)
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
