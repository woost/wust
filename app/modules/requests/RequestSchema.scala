package modules.requests

import model.WustSchema._
import modules.db._
import modules.db.access._
import renesca.schema._

case class ApiDefinition(restRoot: String, websocketRoot: String)

trait NodeSchemaBase[NODE <: UuidNode] {
  val connectSchemas: Map[String, ConnectSchema[NODE]]
}

case class NodeSchema[
  NODE <: UuidNode
](path: String, op: NodeAccess[NODE], connectSchemas: Map[String, ConnectSchema[NODE]]) extends NodeSchemaBase[NODE] {
  def toNodeDefinition(uuid: String) = FactoryUuidNodeDefinition(op.factory, uuid)
}

sealed trait ConnectSchema[NODE <: UuidNode] {
  val cardinality = "hasMany"
  val op: RelationAccess[NODE,_ <: UuidNode]
}

sealed trait PlainConnectSchema[BASE <: UuidNode] extends ConnectSchema[BASE]
sealed trait HyperConnectSchema[BASE <: UuidNode] extends ConnectSchema[BASE]

case class StartConnectSchema[
  START <: UuidNode,
  RELATION <: AbstractRelation[START, END],
  END <: UuidNode
](op: StartRelationAccess[START,RELATION,END]) extends PlainConnectSchema[START]

case class EndConnectSchema[
  START <: UuidNode,
  RELATION <: AbstractRelation[START, END],
  END <: UuidNode
](op: EndRelationAccess[START,RELATION,END]) extends PlainConnectSchema[END]

case class StartHyperConnectSchema[
  START <: UuidNode,
  RELATION <: AbstractRelation[START, END] with UuidNode,
  END <: UuidNode
](factory: AbstractRelationFactory[START,RELATION,END] with NodeFactory[RELATION], op: StartRelationAccess[START,RELATION,END], connectSchemas: Map[String, PlainConnectSchema[RELATION]]) extends NodeSchemaBase[RELATION] with HyperConnectSchema[START] {
  def toNodeDefinition(baseDef: UuidNodeDefinition[START], uuid: String) = {
    HyperNodeDefinition(baseDef, factory, op.toNodeDefinition(uuid))
  }
}

case class EndHyperConnectSchema[
  START <: UuidNode,
  RELATION <: AbstractRelation[START, END] with UuidNode,
  END <: UuidNode
](factory: AbstractRelationFactory[START,RELATION,END] with NodeFactory[RELATION], op: EndRelationAccess[START,RELATION,END], connectSchemas: Map[String, PlainConnectSchema[RELATION]]) extends NodeSchemaBase[RELATION] with HyperConnectSchema[END] {
  def toNodeDefinition(baseDef: UuidNodeDefinition[END], uuid: String) = {
    HyperNodeDefinition(op.toNodeDefinition(uuid), factory, baseDef)
  }
}

object StartConnection {
  def unapply[NODE <: UuidNode](schema: ConnectSchema[NODE]) = schema match {
    case StartConnectSchema(op)                => Some(op)
    case StartHyperConnectSchema(_,op,_)       => Some(op)
    case _                                     => None
  }
}

object EndConnection {
  def unapply[NODE <: UuidNode](schema: ConnectSchema[NODE]) = schema match {
    case EndConnectSchema(op)                  => Some(op)
    case EndHyperConnectSchema(_,op,_)         => Some(op)
    case _                                     => None
  }
}

object HyperConnection {
  def unapply[NODE <: UuidNode](schema: ConnectSchema[NODE]) = schema match {
    case StartHyperConnectSchema(_,_,schemas)  => Some(schemas)
    case EndHyperConnectSchema(_,_,schemas)    => Some(schemas)
    case _                                     => None
  }
}
