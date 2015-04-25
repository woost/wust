package formatters.json

import collection.mutable
import modules.requests._
import play.api.libs.json._
import renesca.graph.Graph
import renesca.graph._
import renesca.parameter.implicits._
import renesca.parameter.PropertyKey._
import renesca.parameter.StringPropertyValue
import model._
import model.WustSchema._
import renesca.schema._

object GraphFormat {
  implicit def LabelToString(label: Label): String = label.name
  implicit def RelationTypeToString(relationType: RelationType): String = relationType.name

  implicit object DiscourseRelationFormat extends Format[SchemaRelation[ContentNode, ContentNode]] {
    def reads(json: JsValue) = ???

    def writes(relation: SchemaRelation[ContentNode, ContentNode]) = JsObject(Seq(
      ("startId", JsString(relation.startNode.uuid)),
      ("label", JsString(relation.relationType)),
      ("endId", JsString(relation.endNode.uuid))
    ))
  }

  implicit object ContentNodeFormat extends Format[ContentNode] {
    def reads(json: JsValue) = ???

    def writes(node: ContentNode) = JsObject(Seq(
      ("id", JsString(node.uuid)),
      ("label", JsString(node.label)),
      ("title", JsString(node.title))
    ))
  }

  implicit object ContentHyperRelationFormat extends Format[SchemaHyperRelation[ContentNode, _ <: SchemaRelation[ContentNode, _], _ <: SchemaHyperRelation[ContentNode, _, _, _, ContentNode] with UuidNode, _ <: SchemaRelation[_, ContentNode], ContentNode] with UuidNode] {
    def reads(json: JsValue) = ???

    def writes(hyperRelation: SchemaHyperRelation[ContentNode, _ <: SchemaRelation[ContentNode, _], _ <: SchemaHyperRelation[ContentNode, _, _, _, ContentNode] with UuidNode, _ <: SchemaRelation[_, ContentNode], ContentNode] with UuidNode) = {
      JsObject(Seq(
        ("id", JsString(hyperRelation.uuid)),
        ("startId", JsString(hyperRelation.startNode.uuid)),
        ("label", JsString(hyperRelation.label)),
        ("endId", JsString(hyperRelation.endNode.uuid))
      ))
    }
  }

  implicit object DiscourseFormat extends Format[Discourse] {
    def reads(json: JsValue) = ???

    def writes(discourseGraph: Discourse) = {
      JsObject(Seq(
        ("nodes", Json.toJson(discourseGraph.contentNodes)),
        ("relations", Json.toJson(discourseGraph.contentNodeRelations)),
        ("hyperRelations", Json.toJson(discourseGraph.contentNodeHyperRelations))
      ))
    }
  }

  implicit object NodeAddFormat extends Format[NodeAddRequest] {
    def reads(json: JsValue) = json match {
      case JsObject(_) => {
        JsSuccess(NodeAddRequest((json \ "title").as[String]))
      }
      case otherwise   => JsError()
    }

    def writes(nodeAdd: NodeAddRequest) = ???
  }

  implicit object ConnectFormat extends Format[ConnectRequest] {
    def reads(json: JsValue) = json match {
      case JsObject(_) => {
        JsSuccess(ConnectRequest((json \ "id").as[String]))
      }
      case otherwise   => JsError()
    }

    def writes(connect: ConnectRequest) = ???
  }

  implicit def apiDefinitionWrites = new Writes[ApiDefinition] {
    def writes(apiDefinition: ApiDefinition) = JsObject(Seq(
      ("restRoot", JsString(apiDefinition.restRoot)),
      ("websocketRoot", JsString(apiDefinition.websocketRoot))
    ))
  }

  implicit def nodeSchemaWrites[NODE <: ContentNode] = new Writes[NodeSchema[NODE]] {
    // TODO: duplicate code
    implicit def simpleConnectSchemaWrites[SCHEMANODE <: SchemaNode] = new Writes[Map[String,SimpleConnectSchema[SCHEMANODE]]] {
      def writes(schemas: Map[String,SimpleConnectSchema[SCHEMANODE]]) = JsObject(schemas.map {
        case (k,v) => (k, JsObject(Seq(
          ("cardinality", JsString(v.cardinality))
        )))
      }.toList
      )
    }

    implicit def connectSchemaWrites[SCHEMANODE <: SchemaNode] = new Writes[Map[String,ConnectSchema[SCHEMANODE]]] {
      def writes(schemas: Map[String,ConnectSchema[SCHEMANODE]]) = JsObject(schemas.map {
        case (k, v: SimpleConnectSchema[SCHEMANODE]) => (k, JsObject(Seq(
          ("cardinality", JsString(v.cardinality))
        )))
        case (k, v @ StartHyperConnectSchema(_,connectSchemas)) => (k, JsObject(Seq(
          ("cardinality", JsString(v.cardinality)),
          ("subs", Json.toJson(connectSchemas))
        )))
        case (k, v @ EndHyperConnectSchema(_,connectSchemas)) => (k, JsObject(Seq(
          ("cardinality", JsString(v.cardinality)),
          ("subs", Json.toJson(connectSchemas))
        )))
      }.toList
      )
    }

    def writes(schema: NodeSchema[NODE]) = JsObject(Seq(
      ("label", JsString(schema.factory.label)),
      ("path", JsString(schema.path)),
      ("name", JsString(schema.name)),
      ("subs", Json.toJson(schema.connectSchemas))
    ))
  }
}
