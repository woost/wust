package formatters.json

import model.WustSchema._
import modules.requests._
import play.api.libs.json._

object ApiSchemaFormat {

  implicit def apiDefinitionWrites = new Writes[ApiDefinition] {
    def writes(apiDefinition: ApiDefinition) = JsObject(Seq(
      ("restRoot", JsString(apiDefinition.restRoot)),
      ("websocketRoot", JsString(apiDefinition.websocketRoot))
    ))
  }

  implicit def nodeSchemaWrites[NODE <: UuidNode] = new Writes[NodeSchema[NODE]] {
    // TODO: duplicate code
    implicit def simpleConnectSchemaWrites[SCHEMANODE <: UuidNode] = new Writes[Map[String, PlainConnectSchema[SCHEMANODE]]] {
      def writes(schemas: Map[String, PlainConnectSchema[SCHEMANODE]]) = JsObject(schemas.mapValues {
        case v => JsObject(Seq(
          ("cardinality", JsString(v.cardinality))
        ))
      }.toList)
    }

    implicit def connectSchemaWrites[SCHEMANODE <: UuidNode] = new Writes[Map[String, ConnectSchema[SCHEMANODE]]] {
      def writes(schemas: Map[String, ConnectSchema[SCHEMANODE]]) = JsObject(schemas.mapValues {
        case v: PlainConnectSchema[SCHEMANODE]               => JsObject(Seq(
          ("cardinality", JsString(v.cardinality))
        ))
        case v@StartHyperConnectSchema(_, _, connectSchemas) => JsObject(Seq(
          ("cardinality", JsString(v.cardinality)),
          ("subs", Json.toJson(connectSchemas))
        ))
        case v@EndHyperConnectSchema(_, _, connectSchemas)   => JsObject(Seq(
          ("cardinality", JsString(v.cardinality)),
          ("subs", Json.toJson(connectSchemas))
        ))
      }.toList)
    }

    def writes(schema: NodeSchema[NODE]) = JsObject(Seq(
      ("label", JsString(schema.op.factory.label.name)),
      ("path", JsString(schema.path)),
      ("name", JsString(schema.op.name)),
      ("subs", Json.toJson(schema.connectSchemas))
    ))
  }
}
