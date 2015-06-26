package formatters.json

import model.WustSchema._
import modules.requests._
import play.api.libs.json._
import renesca.graph.{Label, RelationType}
import renesca.schema._

object GraphFormat {
  implicit def LabelToString(label: Label): String = label.name
  implicit def RelationTypeToString(relationType: RelationType): String = relationType.name

  implicit object ConnectsFormat extends Format[Relation[Connectable,Connectable]] {
    def reads(json: JsValue) = ???

    def writes(relation: Relation[Connectable,Connectable]) = JsObject(Seq(
      ("startId", JsString(relation.startNode.uuid)),
      ("label", JsString(relation.relationType)),
      ("endId", JsString(relation.endNode.uuid))
    ))
  }

  implicit object NodeFormat extends Format[Connectable] {
    def reads(json: JsValue) = ???

    implicit def tagWrites = new Writes[Tag] {
      def writes(tag: Tag) = JsObject(Seq(
        ("id", JsString(tag.uuid)),
        ("label", JsString(tag.label)),
        ("title", JsString(tag.title.getOrElse(""))),
        ("description", JsString(tag.description)),
        ("isType", JsBoolean(tag.isType))
      ))
    }

    def writes(node: Connectable) = JsObject(Seq(
      ("id", JsString(node.uuid)),
      ("label", JsString(node.label))
      ) ++ (node match {
        case n: Post                         => Seq(
          ("title", JsString(n.title.getOrElse(""))),
          ("description", JsString(n.description)),
          ("tags", Json.toJson(n.rev_categorizes))
        )
      case h: HyperRelation[Connectable @unchecked, _, _, _, Connectable @unchecked] =>
        Seq(
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
        ("nodes", Json.toJson(discourseGraph.posts ++ discourseGraph.connects)),
        ("edges", Json.toJson(discourseGraph.connects.flatMap(r => List(r.startRelation.asInstanceOf[Relation[Connectable, Connectable]], r.endRelation.asInstanceOf[Relation[Connectable, Connectable]]))))
      ))
    }
  }
}
