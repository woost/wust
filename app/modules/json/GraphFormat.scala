package modules.json

import play.api.libs.json._
import renesca.graph.Graph
import renesca.graph._
import renesca.parameter.implicits._
import renesca.parameter.PropertyKey._
import renesca.parameter.StringPropertyValue
import model._

object GraphFormat {
  implicit def LabelToString(label: Label): String = label.name
  implicit def RelationTypeToString(relationType: RelationType): String = relationType.name
  implicit def IdToLong(id: Id): BigDecimal = id.value

  implicit object NodeFormat extends Format[Node] {
    def reads(json: JsValue) = json match {
      case JsObject(_) => {
        val node = Node.local
        node.labels += (json \ "type").as[String]
        node.properties += ("text" -> (json \ "text").as[String])
        node.properties += ("label" -> (json \ "label").as[String])
        JsSuccess(node)
      }
      case _           => JsError()
    }

    def writes(node: Node) = JsObject(Seq(
      ("id", JsNumber(node.id)),
      ("type", JsString(node.labels.headOption.getOrElse("").toString)),
      ("label", JsString(node.properties.getOrElse("label", StringPropertyValue("")).asInstanceOf[StringPropertyValue])),
      ("text", JsString(node.properties.getOrElse("text", StringPropertyValue("")).asInstanceOf[StringPropertyValue]))
    ))
  }

  implicit object RelationFormat extends Format[Relation] {
    def reads(json: JsValue) = ???

    def writes(relation: Relation) = JsObject(Seq(
      ("label", JsString(relation.relationType)),
      ("from", JsNumber(relation.startNode.id)),
      ("to", JsNumber(relation.endNode.id))
    ))
  }

  implicit object UserFormat extends Format[Graph] {
    def reads(json: JsValue) = ???

    def writes(graph: Graph) = JsObject(Seq(
      ("nodes", JsArray(graph.nodes.toList map (Json.toJson(_)))),
      ("edges", JsArray(graph.relations.toList map (Json.toJson(_))))
    ))
  }

  implicit object DiscourseFormat extends Format[DiscourseNode] {
    def reads(json: JsValue) = ???
    def writes(node: DiscourseNode) = JsObject(Seq(
      ("title", JsString(node.title))
    ))
  }

}
