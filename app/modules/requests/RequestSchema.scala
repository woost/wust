package modules.requests

import renesca.schema._
import model.WustSchema._

case class ApiDefinition(restRoot: String, websocketRoot: String)

sealed trait ConnectSchema[BASENODE <: ContentNode] {
  val cardinality = "hasMany"
}

case class StartConnectSchema[START <: ContentNode, RELATION <: SchemaAbstractRelation[START,END] with SchemaItem, END <: ContentNode](factory: SchemaAbstractRelationFactory[START,RELATION,END]) extends ConnectSchema[START]
case class EndConnectSchema[START <: ContentNode, RELATION <: SchemaAbstractRelation[START,END] with SchemaItem, END <: ContentNode](factory: SchemaAbstractRelationFactory[START,RELATION,END]) extends ConnectSchema[END]

case class NodeSchema[BASENODE <: ContentNode](path: String, factory: ContentNodeFactory[BASENODE], connectSchemas: Map[String,ConnectSchema[BASENODE]]) {
  val name = factory.getClass.getSimpleName.dropRight(1)
}
