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
        // the same as tagWrites but with voting weight
        val tag: TagLike = cat.startNodeOpt.get
        val weight: Double = cat.rawItem.properties.get("weight").map(_.asDouble).getOrElse(0)
        JsObject(Seq(
          ("id", JsString(tag.uuid)),
          ("label", JsString(TagLike.label)),
          ("title", JsString(tag.title)),
          ("description", JsString(tag.description.getOrElse(""))),
          ("isVotable", JsBoolean(tag.isInstanceOf[VoteDimension])),
          ("isClassification", JsBoolean(tag.isInstanceOf[Classification])),
          ("color", tag.color.map(JsNumber(_)).getOrElse(JsNull)),
          ("symbol", tag.symbol.map(JsString(_)).getOrElse(JsNull)),
          ("weight", JsNumber(weight))
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
        ("tags", Json.toJson(n.inRelationsAs(Tags).map(tagsWrites.writes))) // TODO: why do we have to call tagsWrites.writes explicitly?
      )
      case n: TagLike => Seq(
        ("id", JsString(n.uuid)),
        ("label", JsString(TagLike.label)),
        ("title", JsString(n.title)),
        ("description", JsString(n.description.getOrElse(""))),
        ("isVotable", JsBoolean(n.isInstanceOf[VoteDimension])),
        ("isClassification", JsBoolean(n.isInstanceOf[Classification])),
        ("color", n.color.map(JsNumber(_)).getOrElse(JsNull))
      )
      case n: RealUser        => Seq(
        ("id", JsString(n.uuid)),
        ("name", JsString(n.name)),
        ("email", JsString(n.email.getOrElse("")))
      )
      case n: DummyUser        => Seq(
        ("id", JsString(n.uuid)),
        ("name", JsString(n.name))
      )
      case n              =>
        throw new RuntimeException("You did not define a formatter for the api: " + node)
    })
    }
  }
}
