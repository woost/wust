package formatters.json

import model.WustSchema._
import play.api.libs.json._
import renesca.graph.{Label, RelationType}
import renesca.schema._
import renesca.parameter._
import renesca.parameter.implicits._

object ApiNodeFormat {
  implicit object NodeFormat extends Format[Node] {
    def reads(json: JsValue) = ???

    def writes(node: Node) = node match {
      case n: Post => PostFormat.NodeFormat.writes(n)
      case n: User => UserFormat.NodeFormat.writes(n)
      case n: Scope => TagFormat.ScopeFormat.writes(n)
      case n: Classification => TagFormat.ClassificationFormat.writes(n)
      case _ => throw new Exception("You did not specify a formatter for the api: " + node)
    }
  }
}
