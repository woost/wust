package modules.json

import modules.requests.ProblemAdd
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

  implicit object DiscourseFormat extends Format[Discourse] {
    def reads(json: JsValue) = ???

    def writes(graph: Discourse) = JsObject(Seq(
      ("nodes", JsArray(graph.discourseNodes.toList map (Json.toJson(_)))),
      ("edges", JsArray(graph.discourseRelations.toList map (Json.toJson(_))))
    ))
  }

  implicit object DiscourseRelationFormat extends Format[DiscourseRelation[DiscourseNode, DiscourseNode]] {
    def reads(json: JsValue) = ???

    def writes(relation: DiscourseRelation[DiscourseNode, DiscourseNode]) = JsObject(Seq(
      ("label", JsString(relation.relationType)),
      ("from", JsString(relation.startNode.uuid)),
      ("to", JsString(relation.endNode.uuid))
    ))
  }

  implicit object DiscourseNodeFormat extends Format[DiscourseNode] {
    def reads(json: JsValue) = ???

    def writes(node: DiscourseNode) = JsObject(Seq(
      ("uuid", JsString(node.uuid)),
      ("title", JsString(node.title)),
      ("label", JsString(node.label))
    ))
  }

  implicit object ProblemAddFormat extends Format[ProblemAdd] {
    def reads(json: JsValue) = json match {
      case JsObject(_) => {
        JsSuccess(ProblemAdd((json \ "title").as[String]))
      }
      case otherwise => JsError()
    }

    def writes(problemAdd: ProblemAdd) = ???
  }

}
