package formatters.json

import model.WustSchema._
import modules.requests._
import play.api.libs.json._
import renesca.graph.{Label, RelationType}
import renesca.schema._

object GraphFormat {
  implicit def LabelToString(label: Label): String = label.name
  implicit def RelationTypeToString(relationType: RelationType): String = relationType.name

  implicit object RelationFormat extends Format[Relation[PostLike, PostLike]] {
    def reads(json: JsValue) = ???

    def writes(relation: Relation[PostLike, PostLike]) = JsObject(Seq(
      ("startId", JsString(relation.startNode.uuid)),
      ("label", JsString(relation.relationType)),
      ("endId", JsString(relation.endNode.uuid))
    ))
  }

  implicit object NodeFormat extends Format[PostLike] {
    def reads(json: JsValue) = ???

    def writes(node: PostLike) = JsObject(Seq(
      ("id", JsString(node.uuid)),
      ("label", JsString(node.label))
    ) ++ (node match {
      case n: Post                         => Seq(
        ("title", JsString(n.title.getOrElse(""))),
        ("description", JsString(n.description)),
        ("tags", Json.toJson(n.rev_categorizesPosts))
      )
      case n: Tag                         => Seq(
        ("title", JsString(n.title.getOrElse(""))),
        ("description", JsString(n.description)),
        ("isType", JsBoolean(n.isType))
      )
      case h: HyperRelation[PostLike @unchecked, _, _, _, PostLike @unchecked] =>
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
        ("edges", Json.toJson(discourseGraph.connects.flatMap(r => List(r.startRelation.asInstanceOf[Relation[PostLike, PostLike]], r.endRelation.asInstanceOf[Relation[PostLike, PostLike]]))))
      ))
    }
  }
}
