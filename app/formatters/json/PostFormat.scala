package formatters.json

import model.WustSchema._
import play.api.libs.json._
import formatters.json.TagFormat._
import renesca.graph.{Label, RelationType}
import renesca.schema._
import renesca.parameter._
import renesca.parameter.implicits._

object PostFormat {
  implicit object PostFormat extends Format[Post] {
    def reads(json: JsValue) = ???

    def writes(n: Post) = JsObject(Seq(
      ("id", JsString(n.uuid)),
      ("title", JsString(n.title)),
      ("description", JsString(n.description.getOrElse(""))),
      ("tags", Json.toJson(n.inRelationsAs(Tags).sortBy(_.uuid))),
      ("classifications", classificationConnectsWriter(n)),
      ("timestamp", Json.toJson(JsNumber(n.timestamp))),
      ("author", n.rev_createds.headOption.map(u => UserFormat.UserFormat.writes(u)).getOrElse(JsNull)),
      ("viewCount", JsNumber(n.viewCount)),
      ("inDegree", {
        val deg = n.rawItem.properties.get("indegree")
        deg.map(d => JsNumber(d.asLong)).getOrElse(JsNull)
      })
    ))
  }
}
