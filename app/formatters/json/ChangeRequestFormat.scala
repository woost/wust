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
        ("post", n.outRelationsAs(UpdatedEnd).headOption.map(r => Json.toJson(r.endNode)).getOrElse(JsNull)),
        ("oldTitle", JsString(n.oldTitle)),
        ("newTitle", JsString(n.newTitle)),
        ("oldDescription", JsString(n.oldDescription.getOrElse(""))),
        ("newDescription", JsString(n.newDescription.getOrElse(""))),
        ("type", JsString("Edit"))
      )
      case n: Deleted        => Seq(
        ("id", JsString(n.uuid)),
        ("post", n.outRelationsAs(DeletedEnd).headOption.map(r => Json.toJson(r.endNode)).getOrElse(JsNull)),
        ("type", JsString("Delete"))
      )
      case n: AddTags    => Seq(
        ("id", JsString(n.uuid)),
        ("post", n.outRelationsAs(AddTagsEnd).headOption.map(r => Json.toJson(r.endNode)).getOrElse(JsNull)),
        ("tag", n.proposesTags.headOption.map(tag => Json.toJson(tag)).getOrElse(JsNull)),
        ("classifications", Json.toJson(n.proposesClassifys)),
        ("type", JsString("AddTag"))
      )
      case n: RemoveTags    => Seq(
        ("id", JsString(n.uuid)),
        ("post", n.outRelationsAs(RemoveTagsEnd).headOption.map(r => Json.toJson(r.endNode)).getOrElse(JsNull)),
        ("tag", n.proposesTags.headOption.map(tag => Json.toJson(tag)).getOrElse(JsNull)),
        ("classifications", Json.toJson(n.proposesClassifys)),
        ("type", JsString("RemoveTag"))
      )
    })
  }
}

object FinishedChangeRequestFormat {
  import TagFormat._
  import UserFormat._

  implicit object CRFormat extends Format[ChangeRequest] {
    def reads(json: JsValue) = ???

    def writes(node: ChangeRequest) = JsObject(node match {
      case n: Updated        => Seq(
        ("id", JsString(n.uuid)),
        ("author", n.inRelationsAs(UpdatedStart).headOption.map(r => Json.toJson(r.startNode)).getOrElse(JsNull)),
        ("oldTitle", JsString(n.oldTitle)),
        ("newTitle", JsString(n.newTitle)),
        ("oldDescription", JsString(n.oldDescription.getOrElse(""))),
        ("newDescription", JsString(n.newDescription.getOrElse(""))),
        ("timestamp", JsNumber(n.timestamp)),
        ("type", JsString("Edit"))
      )
      case n: Deleted        => Seq(
        ("id", JsString(n.uuid)),
        ("author", n.inRelationsAs(DeletedStart).headOption.map(r => Json.toJson(r.startNode)).getOrElse(JsNull)),
        ("timestamp", JsNumber(n.timestamp)),
        ("type", JsString("Delete"))
      )
      case n: AddTags    => Seq(
        ("id", JsString(n.uuid)),
        ("author", n.inRelationsAs(AddTagsStart).headOption.map(r => Json.toJson(r.startNode)).getOrElse(JsNull)),
        ("tag", n.proposesTags.headOption.map(tag => Json.toJson(tag)).getOrElse(JsNull)),
        ("classifications", Json.toJson(n.proposesClassifys)),
        ("timestamp", JsNumber(n.timestamp)),
        ("type", JsString("AddTag"))
      )
      case n: RemoveTags    => Seq(
        ("id", JsString(n.uuid)),
        ("author", n.inRelationsAs(RemoveTagsStart).headOption.map(r => Json.toJson(r.startNode)).getOrElse(JsNull)),
        ("tag", n.proposesTags.headOption.map(tag => Json.toJson(tag)).getOrElse(JsNull)),
        ("classifications", Json.toJson(n.proposesClassifys)),
        ("timestamp", JsNumber(n.timestamp)),
        ("type", JsString("RemoveTag"))
      )
    })
  }
}
