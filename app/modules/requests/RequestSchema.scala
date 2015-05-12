package modules.requests

import modules.db.RelationDefinition
import renesca.schema._
import model.WustSchema._

case class ApiDefinition(restRoot: String, websocketRoot: String)

trait NodeSchemaBase[NODE <: Node] {
  val factory: NodeFactory[NODE]
  val name = factory.getClass.getSimpleName.dropRight(1)
  def connectSchemas: Map[String, ConnectSchema[NODE]]
}

case class NodeSchema[NODE <: ContentNode](path: String, factory: ContentNodeFactory[NODE], connectSchemas: Map[String,ConnectSchema[NODE]]) extends NodeSchemaBase[NODE]

sealed trait ConnectSchema[+NODE <: Node] {
  val cardinality = "hasMany"
}

sealed trait SimpleConnectSchema[+NODE <: Node] extends ConnectSchema[NODE]

sealed trait HyperConnectSchema[+NODE <: ContentNode] extends ConnectSchema[NODE]

case class StartConnectSchema[START <: Node, RELATION <: ContentRelation[START,END], END <: ContentNode](factory: ContentRelationFactory[START,RELATION,END], nodeFactory: NodeFactory[END]) extends SimpleConnectSchema[START]
case class EndConnectSchema[START <: ContentNode, RELATION <: ContentRelation[START,END], END <: Node](factory: ContentRelationFactory[START,RELATION,END], nodeFactory: NodeFactory[START]) extends SimpleConnectSchema[END]

// TODO: should have type signature with SchemaHyperRelation instead of ContentRelation, but then type inference of the factory does not work in pattern matching. for now assure hyperconnection with SchemaNode
case class StartHyperConnectSchema[START <: ContentNode, RELATION <: ContentRelation[START,END] with Node, END <: ContentNode](factory: ContentRelationFactory[START,RELATION,END] with NodeFactory[RELATION], nodeFactory: NodeFactory[END], connectSchemas: Map[String,SimpleConnectSchema[RELATION]]) extends HyperConnectSchema[START] with NodeSchemaBase[RELATION]
case class EndHyperConnectSchema[START <: ContentNode, RELATION <: ContentRelation[START,END] with Node, END <: ContentNode](factory: ContentRelationFactory[START,RELATION,END] with NodeFactory[RELATION], nodeFactory: NodeFactory[START], connectSchemas: Map[String,SimpleConnectSchema[RELATION]]) extends HyperConnectSchema[END] with NodeSchemaBase[RELATION]

object StartConnection {
  def unapply[NODE <: ContentNode](schema: ConnectSchema[Node]) = schema match {
    case StartConnectSchema(factory,_) => Some(factory)
    case StartHyperConnectSchema(factory,_,_) => Some(factory)
    case _ => None
  }
}

object EndConnection {
  def unapply[NODE <: ContentNode](schema: ConnectSchema[NODE]) = schema match {
    case EndConnectSchema(factory,_) => Some(factory)
    case EndHyperConnectSchema(factory,_,_) => Some(factory)
    case _ => None
  }
}

object HyperConnection {
  def unapply[NODE <: ContentNode](schema: ConnectSchema[NODE]) = schema match {
    case StartHyperConnectSchema(_,_,connectSchemas) => Some(connectSchemas)
    case EndHyperConnectSchema(_,_,connectSchemas) => Some(connectSchemas)
    case _ => None
  }
}
