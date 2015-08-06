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
        ("title", JsString(n.title)),
        ("description", JsString(n.description.getOrElse(""))),
        ("tags", Json.toJson(n.rev_categorizes))
      )
      case n: TagLike => Seq(
        ("id", JsString(n.uuid)),
        //TODO: meh, same as in graphofmrat
        ("label", JsString(TagLike.label)),
        ("title", JsString(n.title)),
        ("description", JsString(n.description.getOrElse(""))),
        ("isType", JsBoolean(n.isType))
      )
      case n: User        => Seq(
        ("id", JsString(n.uuid)),
        ("name", JsString(n.name)),
        ("email", JsString(n.email.getOrElse("")))
      )
      case _              => throw new RuntimeException("You did not define a formatter for the api: " + node)
    })
  }
}
