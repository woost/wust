package formatters.json

import model.WustSchema._
import play.api.libs.json._
import renesca.graph.{Label, RelationType}
import renesca.schema._
import renesca.parameter._
import renesca.parameter.implicits._

// for search api, as we do not know the type of nodes
object ExposedNodeFormat {
  implicit object ExposedNodeFormat extends Format[ExposedNode] {
    def reads(json: JsValue) = ???

    def writes(node: ExposedNode) = node match {
      case n: Post => {
        val formatted = PostFormat.PostFormat.writes(n)
        val indegree = n.rawItem.properties.get("indegree")
        indegree.map { deg =>
          JsObject(formatted.fields ++ Seq(
            ("inDegree", JsNumber(deg.asLong))
          ))
        }.getOrElse(formatted)
      }
      case n: Scope => TagFormat.ScopeFormat.writes(n)
      case n: Classification => TagFormat.ClassificationFormat.writes(n)
      case _ => throw new Exception("You did not specify a formatter for the api: " + node)
    }
  }
}
