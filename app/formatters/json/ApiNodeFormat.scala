package formatters.json

import model.WustSchema._
import play.api.libs.json._
import renesca.graph.{Label, RelationType}
import renesca.schema._
import renesca.parameter._
import renesca.parameter.implicits._

object ApiNodeFormat {
  implicit def LabelToString(label: Label): String = label.name

  implicit object NodeFormat extends Format[Node] {
    def reads(json: JsValue) = ???

    implicit def tagsWrites = new Writes[Tags] {
      //TODO: this is a code dup from GraphForma.scala
      def writes(cat: Tags) = {
        val tag: TagLike = cat.startNodeOpt.get
        JsObject(Seq(
          ("id", JsString(tag.uuid)),
          ("label", JsString(TagLike.label)),
          ("title", JsString(tag.title)),
          ("description", JsString(tag.description.getOrElse(""))),
          ("isClassification", JsBoolean(tag.isInstanceOf[Classification])),
          ("color", JsNumber(tag.color)),
          ("symbol", tag.symbol.map(JsString(_)).getOrElse(JsNull))
        ))
      }
    }


    //TODO: this should be multiple formats...code dup
    def writes(node: Node) = {
      JsObject(node match {
      case n: Post => Seq(
        ("id", JsString(n.uuid)),
        ("label", JsString(n.label)),
        ("title", JsString(n.title)),
        ("description", JsString(n.description.getOrElse(""))),
        ("tags", Json.toJson(n.inRelationsAs(Tags).map(tagsWrites.writes))), // TODO: why do we have to call tagsWrites.writes explicitly?
        ("timestamp", Json.toJson(JsNumber(n.timestamp))),
        ("requestsEdit", Json.toJson(n.inRelationsAs(Updated))), //TODO: accessors for subrelations of hyperrelations, to get connected hypernode
        ("requestsTags", Json.toJson(n.inRelationsAs(UpdatedTags)))
      )
      case n: Connects => Seq(
        ("id", JsString(n.uuid)),
        ("startId", n.startNodeOpt.map(s => JsString(s.uuid)).getOrElse(JsNull)),
        ("endId", n.endNodeOpt.map(e => JsString(e.uuid)).getOrElse(JsNull)),
        ("label", JsString(n.label)),
        ("tags", Json.toJson(n.inRelationsAs(Tags).map(tagsWrites.writes))) // TODO: why do we have to call tagsWrites.writes explicitly?
      )
      case n: TagLike => Seq(
        ("id", JsString(n.uuid)),
        ("label", JsString(TagLike.label)),
        ("title", JsString(n.title)),
        ("description", JsString(n.description.getOrElse(""))),
        ("isClassification", JsBoolean(n.isInstanceOf[Classification])),
        ("color", JsNumber(n.color))
      )
      case n: User        => Seq(
        ("id", JsString(n.uuid)),
        ("name", JsString(n.name)),
        ("email", JsString(n.email.getOrElse("")))
      )
      case n: Updated        => Seq(
        ("id", JsString(n.uuid)),
        ("oldTitle", JsString(n.oldTitle)),
        ("newTitle", JsString(n.newTitle)),
        ("oldDescription", JsString(n.oldDescription.getOrElse(""))),
        ("newDescription", JsString(n.newDescription.getOrElse(""))),
        ("vote", n.inRelationsAs(Votes).headOption.map(vote => JsObject(Seq(("weight", JsNumber(vote.weight))))).getOrElse(JsNull)),
        ("threshold", JsNumber(n.applyThreshold)),
        ("votes", JsNumber(n.applyVotes))
      )
      case n: UpdatedTags    => Seq(
        ("id", JsString(n.uuid)),
        ("tags", Json.toJson(n.inRelationsAs(Tags).map(tagsWrites.writes))),
        ("vote", n.inRelationsAs(Votes).headOption.map(vote => JsObject(Seq(("weight", JsNumber(vote.weight))))).getOrElse(JsNull)),
        ("threshold", JsNumber(n.applyThreshold)),
        ("votes", JsNumber(n.applyVotes))
      )
      case n              =>
        throw new RuntimeException("You did not define a formatter for the api: " + node)
    })
    }
  }
}
