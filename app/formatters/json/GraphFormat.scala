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

    implicit def tagWrites = new Writes[TagLike] {
      def writes(tag: TagLike) = JsObject(Seq(
        ("id", JsString(tag.uuid)),
        ("label", JsString(TagLike.label)),
        ("title", JsString(tag.title)),
        ("description", JsString(tag.description.getOrElse(""))),
        ("isType", JsBoolean(tag.isType)),
        ("color", tag.color.map(JsString(_)).getOrElse(JsNull)),
        ("symbol", tag.symbol.map(JsString(_)).getOrElse(JsNull))
      ))
    }

    def writes(node: Connectable) = JsObject(Seq(
      ("id", JsString(node.uuid)),
      ("label", JsString(node.label))
      ) ++ (node match {
        case n: Post                         => Seq(
          ("title", JsString(n.title)),
          ("description", JsString(n.description.getOrElse(""))),
          ("tags", Json.toJson(n.rev_categorizes)),
          ("timestamp", Json.toJson(JsNumber(n.timestamp)))
        )
      case h: Connects  =>
        Seq(
          ("isHyperRelation", JsBoolean(true)),
          //TODO: jsNull oder besser garnicht senden, aus der Seq rausnehmen und flatten
          ("startId", JsString(h.startNodeOpt.map(_.uuid).getOrElse(""))),
          ("endId", JsString(h.endNodeOpt.map(_.uuid).getOrElse(""))),
          ("tags", Json.toJson(h.rev_categorizes))
        )
      case _                                      => Seq.empty
      }))
  }

  implicit object DiscourseFormat extends Format[Discourse] {
    def reads(json: JsValue) = ???

    def writes(discourseGraph: Discourse) = {
      JsObject(Seq(
        ("nodes", Json.toJson(discourseGraph.posts ++ discourseGraph.connects)),
        ("relations", Json.toJson(discourseGraph.connects.flatMap(r => List(r.startRelationOpt, r.endRelationOpt).flatten.map(_.asInstanceOf[Relation[Connectable, Connectable]]))))
      ))
    }
  }
}
