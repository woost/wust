package formatters.json

import model.WustSchema._
import play.api.libs.json._
import renesca.graph.{Label, RelationType}
import renesca.schema._

object DiscourseNodeFormat {
  implicit def LabelToString(label: Label): String = label.name

  implicit object UuidNodeFormat extends Format[UuidNode] {
    def reads(json: JsValue) = ???

    def writes(node: UuidNode) = JsObject(Seq(
      ("id", JsString(node.uuid)),
      ("label", JsString(node.label))
    ) ++ (node match {
      case n: ContentNode => Seq(
        ("title", JsString(n.title)),
        ("description", JsString(n.description.getOrElse("")))
      )
      case u: User        => Seq(
        ("name", JsString(u.email.getOrElse("")))
      )
      case _              => Seq.empty
    }))
  }
}
