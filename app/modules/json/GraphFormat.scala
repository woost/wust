package modules.json

import play.api.libs.json._
import renesca.graph.Graph
import renesca.graph._
import renesca.parameter.implicits._
import renesca.parameter.PropertyKey._
import renesca.parameter.StringPropertyValue

object GraphFormat {
  private implicit def LabelToString(label: Label):String = label.name
  private implicit def RelationTypeToString(relationType: RelationType):String = relationType.name
  private implicit def IdToLong(id: Id):BigDecimal = id.value

  implicit object NodeFormat extends Format[Node] {
    def reads(json: JsValue) = ???

    def writes(node: Node) = JsObject(Seq(
      ("id", JsNumber(node.id)),
      ("type", JsString(node.labels.head)),
      ("label", JsString(node.properties("text").asInstanceOf[StringPropertyValue]))
    ));
  }

  implicit object RelationFormat extends Format[Relation] {
    def reads(json: JsValue) = ???

    def writes(relation: Relation) = JsObject(Seq(
      ("label", JsString(relation.relationType)),
      ("from", JsNumber(relation.startNode.id)),
      ("to", JsNumber(relation.endNode.id))
    ));
  }

  implicit object UserFormat extends Format[Graph] {
    def reads(json: JsValue) = ???

    def writes(graph: Graph) = JsObject(Seq(
      ("nodes", JsArray(graph.nodes.toList map (Json.toJson(_)))),
      ("edges", JsArray(graph.relations.toList map (Json.toJson(_))))
    ));
  }
}
