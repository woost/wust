package formatters.json

import model.WustSchema._
import play.api.libs.json._
import renesca.graph.{Label, RelationType}
import renesca.schema._
import renesca.parameter._
import renesca.parameter.implicits._
import formatters.json.ApiNodeFormat._

object ChangeRequestFormat {

  implicit object CRFormat extends Format[ChangeRequest] {
    def reads(json: JsValue) = ???

    def writes(node: ChangeRequest) = JsObject(node match {
      case n: Updated        => Seq(
        ("id", JsString(n.uuid)),
        ("post", {
          val discourse = Discourse(n.graph)
          n.outRelationsAs(UpdatedToPost).headOption.map(r => Json.toJson(discourse.posts.find(_.uuid == r.endNode.uuid).get)).getOrElse(JsNull)
        }),
        ("oldTitle", JsString(n.oldTitle)),
        ("newTitle", JsString(n.newTitle)),
        ("oldDescription", JsString(n.oldDescription.getOrElse(""))),
        ("newDescription", JsString(n.newDescription.getOrElse(""))),
        ("type", JsString("Edit"))
      )
      case n: AddTags    => Seq(
        ("id", JsString(n.uuid)),
        ("post", {
          val discourse = Discourse(n.graph)
          n.outRelationsAs(AddTagsToPost).headOption.map(r => Json.toJson(discourse.posts.find(_.uuid == r.endNode.uuid).get)).getOrElse(JsNull)
        }),
        ("tag", n.proposesTags.headOption.map(tagWrites.writes).getOrElse(JsNull)),
        ("type", JsString("AddTag"))
      )
      case n: RemoveTags    => Seq(
        ("id", JsString(n.uuid)),
        ("post", {
          val discourse = Discourse(n.graph)
          n.outRelationsAs(RemoveTagsToPost).headOption.map(r => Json.toJson(discourse.posts.find(_.uuid == r.endNode.uuid).get)).getOrElse(JsNull)
        }),
        ("tag", n.proposesTags.headOption.map(tagWrites.writes).getOrElse(JsNull)),
        ("type", JsString("RemoveTag"))
      )
      case n              =>
        throw new RuntimeException("You did not define a formatter for the api: " + node)
    })
  }
}
