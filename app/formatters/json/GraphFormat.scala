package formatters.json

import model.WustSchema._
import modules.requests._
import play.api.libs.json._
import renesca.graph.{Label, RelationType}
import renesca.schema._

object GraphFormat {
  implicit def LabelToString(label: Label): String = label.name
  implicit def RelationTypeToString(relationType: RelationType): String = relationType.name

  implicit object RelationFormat extends Format[Relation[UuidNode, UuidNode]] {
    def reads(json: JsValue) = ???

    def writes(relation: Relation[UuidNode, UuidNode]) = JsObject(Seq(
      ("startId", JsString(relation.startNode.uuid)),
      ("label", JsString(relation.relationType)),
      ("endId", JsString(relation.endNode.uuid))
    ))
  }

  implicit object NodeFormat extends Format[UuidNode] {
    def reads(json: JsValue) = ???

    def writes(node: UuidNode) = JsObject(Seq(
      ("id", JsString(node.uuid)),
      ("label", JsString(node.label))
    ) ++ (node match {
      case n: ContentNode                         => Seq(
        ("title", JsString(n.title))
      )
      case u: User                                => Seq(
        ("title", JsString(u.name))
      )
      case h: ContentRelation[UuidNode, UuidNode] => Seq(
        ("hyperEdge", JsBoolean(true)),
        ("startId", JsString(h.startNode.uuid)),
        ("endId", JsString(h.endNode.uuid))
      )
      case _                                      => Seq.empty
    }))
  }

  implicit object DiscourseFormat extends Format[Discourse] {
    def reads(json: JsValue) = ???

    def writes(discourseGraph: Discourse) = {
      JsObject(Seq(
        //TODO: this is really ugly!
        //TODO: right now this returns more edges than needed. (User --> ContentNode), but there is no User
        ("nodes", Json.toJson(discourseGraph.contentNodes ++ discourseGraph.contentNodeHyperRelations)),
        ("edges", Json.toJson(discourseGraph.uuidNodeRelations ++ discourseGraph.uuidNodeHyperRelations.flatMap(r => List(r.startRelation.asInstanceOf[Relation[UuidNode, UuidNode]], r.endRelation.asInstanceOf[Relation[UuidNode, UuidNode]]))))
      ))
    }
  }
}
