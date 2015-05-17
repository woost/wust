package modules.requests

import model.WustSchema._
import modules.db._
import modules.db.access._
import modules.requests.types.AccessibleConnectSchema
import renesca.schema._

package object types {
  type AccessibleConnectSchema[NODE <: UuidNode] = ConnectSchema[NODE] with ConnectSchemaAccess[NODE]
}

case class ApiDefinition(restRoot: String, websocketRoot: String)

trait NodeSchemaBase[+NODE <: UuidNode] {
  val connectSchemas: Map[String, ConnectSchema[NODE]]
}

case class NodeSchema[
  NODE <: UuidNode
](path: String, op: NodeAccess[NODE], connectSchemas: Map[String, AccessibleConnectSchema[NODE]]) extends NodeSchemaBase[NODE] {
  def toNodeDefinition(uuid: String) = FactoryUuidNodeDefinition(op.factory, uuid)
}

sealed trait ConnectSchema[+NODE <: Node] {
  val cardinality = "hasMany"
}

sealed trait ConnectSchemaAccess[BASE <: UuidNode] {
  val op: RelationAccess[BASE]
}

sealed trait PlainConnectSchema[BASE <: UuidNode] extends ConnectSchema[BASE] with ConnectSchemaAccess[BASE]
sealed trait HyperConnectSchema[BASE <: UuidNode, OTHER <: UuidNode] extends ConnectSchema[BASE] with ConnectSchemaAccess[BASE] {
  val connectSchemas: Map[String, PlainConnectSchema[_ <: UuidNode]]
  val op: CombinedRelationAccess[BASE,OTHER]
}

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
](factory: AbstractRelationFactory[START,RELATION,END] with NodeFactory[RELATION], op: StartRelationAccess[START,RELATION,END], connectSchemas: Map[String, PlainConnectSchema[RELATION]]) extends NodeSchemaBase[RELATION] with HyperConnectSchema[START,END] {
  def toNodeDefinition(baseDef: UuidNodeDefinition[START], uuid: String) = {
    HyperNodeDefinition(baseDef, factory, op.toNodeDefinition(uuid))
  }
}

case class EndHyperConnectSchema[
  START <: UuidNode,
  RELATION <: AbstractRelation[START, END] with UuidNode,
  END <: UuidNode
](factory: AbstractRelationFactory[START,RELATION,END] with NodeFactory[RELATION], op: EndRelationAccess[START,RELATION,END], connectSchemas: Map[String, PlainConnectSchema[RELATION]]) extends NodeSchemaBase[RELATION] with HyperConnectSchema[END,START] {
  def toNodeDefinition(baseDef: UuidNodeDefinition[END], uuid: String) = {
    HyperNodeDefinition(op.toNodeDefinition(uuid), factory, baseDef)
  }
}

object StartConnection {
  def unapply[NODE <: Node](schema: ConnectSchema[NODE]) = schema match {
    case StartConnectSchema(op)                => Some(op)
    case StartHyperConnectSchema(_,op,_)       => Some(op)
    case _                                     => None
  }
}

object EndConnection {
  def unapply[NODE <: Node](schema: ConnectSchema[NODE]) = schema match {
    case EndConnectSchema(op)                  => Some(op)
    case EndHyperConnectSchema(_,op,_)         => Some(op)
    case _                                     => None
  }
}

object HyperConnection {
  def unapply[NODE <: Node](schema: ConnectSchema[NODE]) = schema match {
    case StartHyperConnectSchema(_,_,schemas)  => Some(schemas)
    case EndHyperConnectSchema(_,_,schemas)    => Some(schemas)
    case _                                     => None
  }
}
