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
    implicit def connectSchemaWrites[SCHEMANODE <: UuidNode] = new Writes[Map[String, ConnectSchema[SCHEMANODE]]] {
      def writesAgain[N <: UuidNode](schemas: Map[String, ConnectSchema[N]]): JsValue = JsObject(schemas.mapValues( v => {
        JsObject(Seq(
          ("cardinality", JsString(v.cardinality))
        ))
      }).toList)

      def writes(schemas: Map[String, ConnectSchema[SCHEMANODE]]): JsValue = JsObject(schemas.mapValues {
        case v: PlainConnectSchema[SCHEMANODE]               => JsObject(Seq(
          ("cardinality", JsString(v.cardinality))
        ))
        case v@StartHyperConnectSchema(_, _, connectSchemas) => JsObject(Seq(
          ("cardinality", JsString(v.cardinality)),
          ("subs", writesAgain(connectSchemas))
        ))
        case v@EndHyperConnectSchema(_, _, connectSchemas)   => JsObject(Seq(
          ("cardinality", JsString(v.cardinality)),
          ("subs", writesAgain(connectSchemas))
        ))
      }.toList)
    }

    def writes(schema: NodeSchema[NODE]) = JsObject(Seq(
      ("label", JsString(schema.op.label.map(_.name).getOrElse(""))),
      ("path", JsString(schema.path)),
      ("name", JsString(schema.op.name)),
      ("subs", Json.toJson(schema.connectSchemas))
    ))
  }
}
