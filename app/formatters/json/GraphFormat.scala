package formatters.json

import model.WustSchema._
import modules.db.Database
import modules.requests._
import play.api.libs.json._
import renesca.graph.{Label, RelationType}
import renesca.schema._
import renesca.parameter._
import renesca.parameter.implicits._

object GraphFormat {
  implicit def LabelToString(label: Label): String = label.name
  implicit def RelationTypeToString(relationType: RelationType): String = relationType.name

  implicit object ConnectsFormat extends Format[Relation[Connectable, Connectable]] {
    def reads(json: JsValue) = ???

    def writes(relation: Relation[Connectable, Connectable]) = JsObject(Seq(
      ("startId", JsString(relation.startNode.uuid)),
      ("label", JsString(relation.relationType)),
      ("endId", JsString(relation.endNode.uuid))
    ))
  }

  implicit object NodeFormat extends Format[Connectable] {
    def reads(json: JsValue) = ???

    import ApiNodeFormat.{tagWrites, tagsWrites}

    def writes(node: Connectable) = JsObject(Seq(
      ("id", JsString(node.uuid)),
      ("label", JsString(node.label))
    ) ++ (node match {
      case post: Post         => Seq(
        ("title", JsString(post.title)),
        ("description", JsString(post.description.getOrElse(""))),
        ("tags", Json.toJson(post.inRelationsAs(Tags))),
        ("timestamp", Json.toJson(JsNumber(post.timestamp))),
        ("viewcount", JsNumber(post.viewCount))
      )
      case connects: Connects =>
        Seq(
          ("isHyperRelation", JsBoolean(true)),
          //TODO: jsNull oder besser garnicht senden, aus der Seq rausnehmen und flatten
          ("startId", JsString(connects.startNodeOpt.map(_.uuid).getOrElse(""))),
          ("endId", JsString(connects.endNodeOpt.map(_.uuid).getOrElse(""))),
          ("tags", Json.toJson(connects.neighboursAs(Classification).map(tagWrites.writes))),
          ("quality", connects.startNodeOpt.map(post => JsNumber(connects.quality(post.viewCount))).getOrElse(JsNull)),
          ("vote", connects.rawItem.properties.get("selfanswervotecount").map(c => JsObject(Seq(
            ("weight", JsNumber(c.asLong))))).getOrElse(JsNull)
          )
        )
      case _                  => Seq.empty
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
