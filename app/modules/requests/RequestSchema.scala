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
](path: String, op: NodeAccess[NODE], connectSchemas: Map[String, ConnectSchema[NODE]]) extends NodeSchemaBase[NODE]

sealed trait ConnectSchema[-NODE <: UuidNode] {
  val cardinality = "hasMany"
  val op: RelationAccess[NODE, UuidNode]
}

sealed trait PlainConnectSchema[BASE <: UuidNode] extends ConnectSchema[BASE]
sealed trait HyperConnectSchema[BASE <: UuidNode] extends ConnectSchema[BASE]

case class StartConnectSchema[
START <: UuidNode,
RELATION <: AbstractRelation[START, END],
END <: UuidNode
](op: StartRelationAccess[START, RELATION, END]) extends PlainConnectSchema[START]

case class EndConnectSchema[
START <: UuidNode,
RELATION <: AbstractRelation[START, END],
END <: UuidNode
](op: EndRelationAccess[START, RELATION, END]) extends PlainConnectSchema[END]

case class StartHyperConnectSchema[
START <: UuidNode,
RELATION <: AbstractRelation[START, END] with UuidNode,
END <: UuidNode
](factory: AbstractRelationFactory[START, RELATION, END] with NodeFactory[RELATION], op: StartRelationAccess[START, RELATION, END], connectSchemas: Map[String, ConnectSchema[RELATION]]) extends NodeSchemaBase[RELATION] with HyperConnectSchema[START] {
  def toNodeDefinition(baseDef: FixedNodeDefinition[START], uuid: String) = {
    HyperNodeDefinition(baseDef, factory, op.toNodeDefinition(uuid))
  }
}

case class EndHyperConnectSchema[
START <: UuidNode,
RELATION <: AbstractRelation[START, END] with UuidNode,
END <: UuidNode
](factory: AbstractRelationFactory[START, RELATION, END] with NodeFactory[RELATION], op: EndRelationAccess[START, RELATION, END], connectSchemas: Map[String, ConnectSchema[RELATION]]) extends NodeSchemaBase[RELATION] with HyperConnectSchema[END] {
  def toNodeDefinition(baseDef: FixedNodeDefinition[END], uuid: String) = {
    HyperNodeDefinition(op.toNodeDefinition(uuid), factory, baseDef)
  }
}

object dsl {
  object N {
    def apply[NODE <: UuidNode](factory: UuidNodeFactory[NODE], op: NodeAccess[NODE], connectSchemas: (String, UuidNodeFactory[NODE] => ConnectSchema[NODE])*): (String) => NodeSchema[NODE] = {
      (path) => new NodeSchema(path, op, connectSchemas.toMap.mapValues(_(factory)))
    }

    def -->[
    START <: UuidNode,
    RELATION <: AbstractRelation[START, END],
    END <: UuidNode
    ](op: UuidNodeFactory[START] => StartRelationAccess[START, RELATION, END]): (UuidNodeFactory[START]) => StartConnectSchema[START, RELATION, END] = {
      (baseFactory) => new StartConnectSchema(op(baseFactory))
    }

    def <--[
    START <: UuidNode,
    RELATION <: AbstractRelation[START, END],
    END <: UuidNode
    ](op: UuidNodeFactory[END] => EndRelationAccess[START, RELATION, END]): (UuidNodeFactory[END]) => EndConnectSchema[START, RELATION, END] = {
      (baseFactory) => new EndConnectSchema(op(baseFactory))
    }

    def --->[
    START <: UuidNode,
    RELATION <: AbstractRelation[START, END] with UuidNode,
    END <: UuidNode
    ](factory: AbstractRelationFactory[START, RELATION, END] with UuidNodeFactory[RELATION], op: UuidNodeFactory[START] => StartRelationAccess[START, RELATION, END], connectSchemas: (String, (UuidNodeFactory[RELATION]) => ConnectSchema[RELATION])*): UuidNodeFactory[START] => StartHyperConnectSchema[START, RELATION, END] = {
      (baseFactory) => new StartHyperConnectSchema(factory, op(baseFactory), connectSchemas.toMap.mapValues(_(factory)))
    }

    def <---[
    START <: UuidNode,
    RELATION <: AbstractRelation[START, END] with UuidNode,
    END <: UuidNode
    ](factory: AbstractRelationFactory[START, RELATION, END] with UuidNodeFactory[RELATION], op: UuidNodeFactory[END] => EndRelationAccess[START, RELATION, END], connectSchemas: (String, (UuidNodeFactory[RELATION]) => ConnectSchema[RELATION])*): UuidNodeFactory[END] => EndHyperConnectSchema[START, RELATION, END] = {
      (baseFactory) => new EndHyperConnectSchema(factory, op(baseFactory), connectSchemas.toMap.mapValues(_(factory)))
    }
  }
}
