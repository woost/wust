package formatters.json

import model.WustSchema._
import play.api.libs.json._
import renesca.graph.{Label, RelationType}
import renesca.schema._
import renesca.parameter._
import renesca.parameter.implicits._

object ChangeRequestFormat {
  import PostFormat._
  import TagFormat._

  implicit object CRFormat extends Format[ChangeRequest] {
    def reads(json: JsValue) = ???

    def writes(node: ChangeRequest) = JsObject(node match {
      case n: Updated        => Seq(
        ("id", JsString(n.uuid)),
        ("post", n.outRelationsAs(UpdatedToPost).headOption.map(r => Json.toJson(r.endNode)).getOrElse(JsNull)),
        ("oldTitle", JsString(n.oldTitle)),
        ("newTitle", JsString(n.newTitle)),
        ("oldDescription", JsString(n.oldDescription.getOrElse(""))),
        ("newDescription", JsString(n.newDescription.getOrElse(""))),
        ("type", JsString("Edit"))
      )
      case n: Deleted        => Seq(
        ("id", JsString(n.uuid)),
        ("post", n.outRelationsAs(DeletedToPost).headOption.map(r => Json.toJson(r.endNode)).getOrElse(JsNull)),
        ("type", JsString("Delete"))
      )
      case n: AddTags    => Seq(
        ("id", JsString(n.uuid)),
        ("post", n.outRelationsAs(AddTagsToPost).headOption.map(r => Json.toJson(r.endNode)).getOrElse(JsNull)),
        ("tag", n.proposesTags.headOption.map(tag => Json.toJson(tag)).getOrElse(JsNull)),
        ("classifications", Json.toJson(n.proposesClassifys)),
        ("type", JsString("AddTag"))
      )
      case n: RemoveTags    => Seq(
        ("id", JsString(n.uuid)),
        ("post", n.outRelationsAs(RemoveTagsToPost).headOption.map(r => Json.toJson(r.endNode)).getOrElse(JsNull)),
        ("tag", n.proposesTags.headOption.map(tag => Json.toJson(tag)).getOrElse(JsNull)),
        ("classifications", Json.toJson(n.proposesClassifys)),
        ("type", JsString("RemoveTag"))
      )
      case n              =>
        throw new RuntimeException("You did not define a formatter for the api: " + node)
    })
  }
}
