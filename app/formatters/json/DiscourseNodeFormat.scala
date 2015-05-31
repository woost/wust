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
      ("id", JsString(node.uuid))
    ) ++ (node match {
      case n: ContentNode => Seq(
        ("label", JsString(n.label)),
        ("title", JsString(n.title.getOrElse(""))),
        ("description", JsString(n.description))
      )
      case u: User        => Seq(
        ("name", JsString(u.name)),
        ("email", JsString(u.email.getOrElse("")))
      )
      case _              => Seq.empty
    }))
  }
}
