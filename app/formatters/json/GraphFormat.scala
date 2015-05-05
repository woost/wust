package formatters.json

import model.WustSchema._
import modules.requests._
import play.api.libs.json._
import renesca.graph.{Label, RelationType}
import renesca.schema._

object GraphFormat {
  implicit def LabelToString(label: Label): String = label.name
  implicit def RelationTypeToString(relationType: RelationType): String = relationType.name

  implicit object DiscourseRelationFormat extends Format[Relation[UuidNode, UuidNode]] {
    def reads(json: JsValue) = ???

    def writes(relation: Relation[UuidNode, UuidNode]) = JsObject(Seq(
      ("startId", JsString(relation.startNode.uuid)),
      ("label", JsString(relation.relationType)),
      ("endId", JsString(relation.endNode.uuid))
    ))
  }

  implicit object ContentNodeFormat extends Format[UuidNode] {
    def reads(json: JsValue) = ???

    //TODO: different formatter for /graph and /problems|goals|ideas
    def writes(node: UuidNode) = JsObject(Seq(
      ("id", JsString(node.uuid)),
      ("label", JsString(node.label))
    ) ++ (node match {
      case n: ContentNode => Seq(
        ("title", JsString(n.title)),
        ("description", JsString(n.description.getOrElse(""))),
        ("hyperEdge", JsBoolean(false))
      )
      case _              => Seq(
        ("hyperEdge", JsBoolean(true))
      )
    }
      ))
  }

  implicit object DiscourseFormat extends Format[Discourse] {
    def reads(json: JsValue) = ???

    def writes(discourseGraph: Discourse) = {
      JsObject(Seq(
        //TODO: this is really ugly!
        ("nodes", Json.toJson(discourseGraph.uuidNodes ++ discourseGraph.uuidNodeHyperRelations)),
        ("edges", Json.toJson(discourseGraph.uuidNodeRelations ++ discourseGraph.uuidNodeHyperRelations.flatMap(r => List(r.startRelation.asInstanceOf[Relation[UuidNode, UuidNode]], r.endRelation.asInstanceOf[Relation[UuidNode, UuidNode]]))))
      ))
    }
  }

  implicit object NodeAddFormat extends Format[NodeAddRequest] {
    def reads(json: JsValue) = json match {
      case JsObject(_) => {
        JsSuccess(NodeAddRequest((json \ "title").as[String], (json \ "description").as[Option[String]]))
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

  //TODO: move into different file --- FROM HERE
  implicit def apiDefinitionWrites = new Writes[ApiDefinition] {
    def writes(apiDefinition: ApiDefinition) = JsObject(Seq(
      ("restRoot", JsString(apiDefinition.restRoot)),
      ("websocketRoot", JsString(apiDefinition.websocketRoot))
    ))
  }

  implicit def nodeSchemaWrites[NODE <: ContentNode] = new Writes[NodeSchema[NODE]] {
    // TODO: duplicate code
    implicit def simpleConnectSchemaWrites[SCHEMANODE <: Node] = new Writes[Map[String, SimpleConnectSchema[SCHEMANODE]]] {
      def writes(schemas: Map[String, SimpleConnectSchema[SCHEMANODE]]) = JsObject(schemas.map {
        case (k, v) => (k, JsObject(Seq(
          ("cardinality", JsString(v.cardinality))
        )))
      }.toList
      )
    }

    implicit def connectSchemaWrites[SCHEMANODE <: Node] = new Writes[Map[String, ConnectSchema[SCHEMANODE]]] {
      def writes(schemas: Map[String, ConnectSchema[SCHEMANODE]]) = JsObject(schemas.map {
        case (k, v: SimpleConnectSchema[SCHEMANODE])           => (k, JsObject(Seq(
          ("cardinality", JsString(v.cardinality))
        )))
        case (k, v@StartHyperConnectSchema(_, connectSchemas)) => (k, JsObject(Seq(
          ("cardinality", JsString(v.cardinality)),
          ("subs", Json.toJson(connectSchemas))
        )))
        case (k, v@EndHyperConnectSchema(_, connectSchemas))   => (k, JsObject(Seq(
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
