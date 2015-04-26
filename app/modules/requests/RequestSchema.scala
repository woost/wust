package modules.requests

import renesca.schema._
import model.WustSchema._

case class ApiDefinition(restRoot: String, websocketRoot: String)

trait NodeSchemaBase[NODE <: SchemaNode] {
  val factory: SchemaNodeFactory[NODE]
  val name = factory.getClass.getSimpleName.dropRight(1)
  def connectSchemas: Map[String, ConnectSchema[NODE]]
}

case class NodeSchema[NODE <: ContentNode](path: String, factory: ContentNodeFactory[NODE], connectSchemas: Map[String,ConnectSchema[NODE]]) extends NodeSchemaBase[NODE]

sealed trait ConnectSchema[NODE <: SchemaNode] {
  val cardinality = "hasMany"
}

sealed trait SimpleConnectSchema[NODE <: SchemaNode] extends ConnectSchema[NODE]
sealed trait HyperConnectSchema[NODE <: ContentNode] extends ConnectSchema[NODE]

case class StartConnectSchema[START <: SchemaNode, RELATION <: SchemaAbstractRelation[START,END] with SchemaItem, END <: ContentNode](factory: SchemaAbstractRelationFactory[START,RELATION,END]) extends SimpleConnectSchema[START]
case class EndConnectSchema[START <: ContentNode, RELATION <: SchemaAbstractRelation[START,END] with SchemaItem, END <: SchemaNode](factory: SchemaAbstractRelationFactory[START,RELATION,END]) extends SimpleConnectSchema[END]

// TODO: should have type signature with SchemaHyperRelation instead of SchemaAbstractRelation, but then type inference of the factory does not work in pattern matching. for now assure hyperconnection with SchemaNode
case class StartHyperConnectSchema[START <: ContentNode, RELATION <: SchemaAbstractRelation[START,END] with SchemaNode, END <: ContentNode](override val factory: SchemaAbstractRelationFactory[START,RELATION,END] with SchemaNodeFactory[RELATION], override val connectSchemas: Map[String,SimpleConnectSchema[RELATION]]) extends HyperConnectSchema[START] with NodeSchemaBase[RELATION]
case class EndHyperConnectSchema[START <: ContentNode, RELATION <: SchemaAbstractRelation[START,END] with SchemaNode, END <: ContentNode](override val factory: SchemaAbstractRelationFactory[START,RELATION,END] with SchemaNodeFactory[RELATION], override val connectSchemas: Map[String,SimpleConnectSchema[RELATION]]) extends HyperConnectSchema[END] with NodeSchemaBase[RELATION]

object StartConnection {
  def unapply[START <: SchemaNode](schema: ConnectSchema[START]) = schema match {
    case StartConnectSchema(factory) => Some(factory)
    case StartHyperConnectSchema(factory,_) => Some(factory)
    case _ => None
  }
}

object EndConnection {
  def unapply[END <: ContentNode](schema: ConnectSchema[END]) = schema match {
    case EndConnectSchema(factory) => Some(factory)
    case EndHyperConnectSchema(factory,_) => Some(factory)
    case _ => None
  }
}

object HyperConnection {
  def unapply[END <: ContentNode](schema: ConnectSchema[END]) = schema match {
    case StartHyperConnectSchema(_,connectSchemas) => Some(connectSchemas)
    case EndHyperConnectSchema(_,connectSchemas) => Some(connectSchemas)
    case _ => None
  }
}
