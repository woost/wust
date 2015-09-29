package formatters.json

import model.WustSchema._
import play.api.libs.json._
import renesca.graph.{Label, RelationType}
import renesca.schema._
import renesca.parameter._
import renesca.parameter.implicits._
import formatters.json.TagFormat._

object EditNodeFormat {

  implicit object CRFormat extends Format[ChangeRequest] {
    def reads(json: JsValue) = ???

    def writes(node: ChangeRequest) = JsObject(node match {
      case n: Updated        => Seq(
        ("id", JsString(n.uuid)),
        ("oldTitle", JsString(n.oldTitle)),
        ("newTitle", JsString(n.newTitle)),
        ("oldDescription", JsString(n.oldDescription.getOrElse(""))),
        ("newDescription", JsString(n.newDescription.getOrElse(""))),
        ("vote", n.inRelationsAs(Votes).headOption.map(vote => JsObject(Seq(("weight", JsNumber(vote.weight))))).getOrElse(JsNull)),
        ("applyThreshold", JsNumber(n.applyThreshold)),
        ("rejectThreshold", JsNumber(n.rejectThreshold)),
        ("votes", JsNumber(n.approvalSum)),
        ("status", JsNumber(n.status)),
        ("type", JsString("Edit"))
      )
      case n: Deleted        => Seq(
        ("id", JsString(n.uuid)),
        ("vote", n.inRelationsAs(Votes).headOption.map(vote => JsObject(Seq(("weight", JsNumber(vote.weight))))).getOrElse(JsNull)),
        ("applyThreshold", JsNumber(n.applyThreshold)),
        ("rejectThreshold", JsNumber(n.rejectThreshold)),
        ("votes", JsNumber(n.approvalSum)),
        ("status", JsNumber(n.status)),
        ("type", JsString("Delete"))
      )
      case n: AddTags    => Seq(
        ("id", JsString(n.uuid)),
        ("tag", n.proposesTags.headOption.map(t => Json.toJson(t)).getOrElse(JsNull)),
        ("classifications", Json.toJson(n.proposesClassifys)),
        ("vote", n.inRelationsAs(Votes).headOption.map(vote => JsObject(Seq(("weight", JsNumber(vote.weight))))).getOrElse(JsNull)),
        ("applyThreshold", JsNumber(n.applyThreshold)),
        ("rejectThreshold", JsNumber(n.rejectThreshold)),
        ("votes", JsNumber(n.approvalSum)),
        ("isRemove", JsBoolean(false)),
        ("status", JsNumber(n.status)),
        ("type", JsString("AddTag"))
      )
      case n: RemoveTags    => Seq(
        ("id", JsString(n.uuid)),
        ("tag", n.proposesTags.headOption.map(t => Json.toJson(t)).getOrElse(JsNull)),
        ("classifications", Json.toJson(n.proposesClassifys)),
        ("vote", n.inRelationsAs(Votes).headOption.map(vote => JsObject(Seq(("weight", JsNumber(vote.weight))))).getOrElse(JsNull)),
        ("applyThreshold", JsNumber(n.applyThreshold)),
        ("rejectThreshold", JsNumber(n.rejectThreshold)),
        ("votes", JsNumber(n.approvalSum)),
        ("isRemove", JsBoolean(true)),
        ("status", JsNumber(n.status)),
        ("type", JsString("RemoveTag"))
      )
    })
  }

  implicit object PostFormat extends Format[Post] {
    def reads(json: JsValue) = ???

    def writes(n: Post) = JsObject(Seq(
      ("id", JsString(n.uuid)),
      ("title", JsString(n.title)),
      ("description", JsString(n.description.getOrElse(""))),
      ("tags", Json.toJson(n.inRelationsAs(Tags).sortBy(_.uuid))),
      ("classifications", JsArray(n.outRelationsAs(ConnectsStart).map(_.endNode).flatMap(con => con.rev_classifies.sortBy(_.uuid).map((_, con))).groupBy(_._1).mapValues(_.map(_._2)).map(classificationWriter(n, _)).toSeq)),
      ("timestamp", Json.toJson(JsNumber(n.timestamp))),
      //TODO: merge the three arrays into one?
      ("requestsEdit", Json.toJson(n.inRelationsAs(Updated))),
      ("requestsDelete", Json.toJson(n.inRelationsAs(Deleted))),
      ("requestsTags", Json.toJson(n.inRelationsAs(AddTags) ++ n.inRelationsAs(RemoveTags))),
      ("viewCount", JsNumber(n.viewCount))
    ))
  }

  implicit object ReferenceFormat extends Format[Reference] {
    def reads(json: JsValue) = ???

    def writes(node: Reference) = JsObject(node match {
      case n:Connects => Seq(
        ("id", JsString(n.uuid)),
        // ("startId", n.startNodeOpt.map(s => JsString(s.uuid)).getOrElse(JsNull)),
        // ("endId", n.endNodeOpt.map(e => JsString(e.uuid)).getOrElse(JsNull)),
        // ("quality", n.startNodeOpt.map(post => JsNumber(n.quality(post.viewCount))).getOrElse(JsNull)),
        ("tags", Json.toJson(n.rev_classifies.sortBy(_.uuid)))
      )
      case n:Tags => Seq(
        ("id", JsString(n.uuid)),
        // ("startId", n.startNodeOpt.map(s => JsString(s.uuid)).getOrElse(JsNull)),
        // ("endId", n.endNodeOpt.map(e => JsString(e.uuid)).getOrElse(JsNull)),
        // ("quality", n.endNodeOpt.map(post => JsNumber(n.quality(post.viewCount))).getOrElse(JsNull)),
        ("tags", Json.toJson(n.rev_classifies.sortBy(_.uuid)))
      )
    })
  }
}
