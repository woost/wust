package modules.db.access

import model.WustSchema._
import modules.db.Database._
import modules.db._
import play.api.libs.json.JsValue
import renesca.schema._

trait RelationAccess[NODE <: UuidNode] {
  def read(baseDef: FixedNodeDefinition[NODE]): Either[Set[_ <: UuidNode],String] = Right("No read access on Relation")
  def delete(baseDef: FixedNodeDefinition[NODE], uuid: String): Either[Boolean,String] = Right("No delete access on Relation")
  def create(baseDef: UuidNodeDefinition[NODE], json: JsValue): Either[_ <: UuidNode,String] = Right("No create access on Relation")
  def createHyper(baseDef: HyperNodeDefinitionBase[NODE with AbstractRelation[_,_]], json: JsValue): Either[_ <: UuidNode,String] = Right("No create access on HyperRelation")
}

trait StartRelationAccess[
  START <: UuidNode,
  RELATION <: AbstractRelation[START,END],
  END <: UuidNode
] extends RelationAccess[START] {
  val factory: AbstractRelationFactory[START, RELATION, END]

  def toNodeDefinition: NodeDefinition[END]
  def toNodeDefinition(uuid: String): FixedNodeDefinition[END]
}

trait EndRelationAccess[
  START <: UuidNode,
  RELATION <: AbstractRelation[START,END],
  END <: UuidNode
] extends RelationAccess[END] {
  val factory: AbstractRelationFactory[START, RELATION, END]

  def toNodeDefinition: NodeDefinition[START]
  def toNodeDefinition(uuid: String): FixedNodeDefinition[START]
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
  def toNodeDefinition(uuid: String) = UuidNodeDefinition(nodeFactory, uuid)

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

  override def delete(baseDef: FixedNodeDefinition[START], uuid:String) = {
    val otherNode = UuidNodeDefinition(nodeFactory, uuid)
    disconnectNodes(RelationDefinition(baseDef, factory, otherNode))
    //Broadcaster.broadcastDisconnect(uuid, factory, otherUuid)
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
  def toNodeDefinition(uuid: String) = UuidNodeDefinition(nodeFactory, uuid)

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

  override def delete(baseDef: FixedNodeDefinition[END], uuid:String) = {
    val otherNode = UuidNodeDefinition(nodeFactory, uuid)
    disconnectNodes(RelationDefinition(otherNode, factory, baseDef))
    //Broadcaster.broadcastDisconnect(uuid, factory, otherUuid)
    Left(true)
  }
}
