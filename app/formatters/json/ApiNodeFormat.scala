package formatters.json

import model.WustSchema._
import play.api.libs.json._
import renesca.graph.{Label, RelationType}
import renesca.schema._

object ApiNodeFormat {
  implicit def LabelToString(label: Label): String = label.name

  implicit object NodeFormat extends Format[Node] {
    def reads(json: JsValue) = ???

    //TODO: this should be multiple formats...code dup
    def writes(node: Node) = JsObject(node match {
      case n: Post => Seq(
        ("id", JsString(n.uuid)),
        ("label", JsString(n.label)),
        ("title", JsString(n.title.getOrElse(""))),
        ("description", JsString(n.description)),
        ("tags", Json.toJson(n.rev_categorizes))
      )
      case n: Scope => Seq(
        ("id", JsString(n.uuid)),
        ("label", JsString(n.label)),
        ("title", JsString(n.title.getOrElse(""))),
        ("description", JsString(n.description))
      )
      case n: Tag => Seq(
        ("id", JsString(n.uuid)),
        ("label", JsString(n.label)),
        ("title", JsString(n.title.getOrElse(""))),
        ("description", JsString(n.description)),
        ("isType", JsBoolean(n.isType))
      )
      case n: ContentNode => Seq(
        ("id", JsString(n.uuid)),
        ("label", JsString(n.label)),
        ("title", JsString(n.title.getOrElse(""))),
        ("description", JsString(n.description))
      )
      case n: User        => Seq(
        ("id", JsString(n.uuid)),
        ("name", JsString(n.name)),
        ("email", JsString(n.email.getOrElse("")))
      )
      case _              => Seq.empty
    })
  }
}
