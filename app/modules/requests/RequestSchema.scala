package modules.requests

import renesca.schema._
import model.WustSchema._

sealed trait RequestSchema

sealed trait ConnectSchema[BASENODE <: ContentNode] extends RequestSchema {
  val cardinality = "hasMany"
}

case class StartConnectSchema[START <: ContentNode, RELATION <: SchemaAbstractRelation[START,END] with SchemaItem, END <: ContentNode](factory: SchemaAbstractRelationFactory[START,RELATION,END]) extends ConnectSchema[START]
case class EndConnectSchema[START <: ContentNode, RELATION <: SchemaAbstractRelation[START,END] with SchemaItem, END <: ContentNode](factory: SchemaAbstractRelationFactory[START,RELATION,END]) extends ConnectSchema[END]

case class NodeSchema[BASENODE <: ContentNode](path: String, factory: ContentNodeFactory[BASENODE], connectSchemas: Map[String,ConnectSchema[BASENODE]]) extends RequestSchema {
  val name = factory.getClass.getSimpleName.dropRight(1)
}
