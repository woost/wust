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

  implicit object SubConnects extends Format[Relation[Connectable, Connectable]] {
    def reads(json: JsValue) = ???

    def writes(relation: Relation[Connectable, Connectable]) = JsObject(Seq(
      ("startId", JsString(relation.startNode.uuid)),
      ("label", JsString(relation.relationType)),
      ("endId", JsString(relation.endNode.uuid))
    ))
  }

  implicit object ConnectsFormat extends Format[Connects] {
    def reads(json: JsValue) = ???

    import TagFormat._

    def writes(connects: Connects) = JsObject(Seq(
      ("id", JsString(connects.uuid)),
      ("label", JsString(connects.label)),
      ("isHyperRelation", JsBoolean(true)),
      //TODO: jsNull oder besser garnicht senden, aus der Seq rausnehmen und flatten
      ("startId", JsString(connects.startNodeOpt.map(_.uuid).getOrElse(""))),
      ("endId", JsString(connects.endNodeOpt.map(_.uuid).getOrElse(""))),
      ("tags", Json.toJson(connects.neighboursAs(Classification))),
      ("timestamp", JsNumber(connects.timestamp)),
      ("quality", connects.startNodeOpt.map(post => JsNumber(connects.quality(post.viewCount))).getOrElse(JsNull)),
      ("vote", connects.rawItem.properties.get("selfanswervotecount").map(c => JsObject(Seq(
        ("weight", JsNumber(c.asLong))))).getOrElse(JsNull)
      )
    ))
  }

  implicit object PostFormat extends Format[Post] {
    def reads(json: JsValue) = ???

    import TagFormat._

    def writes(post: Post) = JsObject(Seq(
      ("id", JsString(post.uuid)),
      ("label", JsString(post.label)),
      ("title", JsString(post.title)),
      ("description", JsString(post.description.getOrElse(""))),
      ("tags", Json.toJson(post.inRelationsAs(Tags))),
      ("timestamp", Json.toJson(JsNumber(post.timestamp))),
      ("author", post.rev_createds.headOption.map(u => UserFormat.UserFormat.writes(u)).getOrElse(JsNull)),
      ("viewCount", JsNumber(post.viewCount))
    ))
  }

  implicit object ConnectableFormat extends Format[Connectable] {
    def reads(json: JsValue) = ???

    def writes(node: Connectable) = node match {
      case post: Post => PostFormat.writes(post)
      case connects: Connects => ConnectsFormat.writes(connects)
    }
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
