package formatters.json

import model.WustSchema._
import play.api.libs.json._
import renesca.graph.{Label, RelationType}
import renesca.schema._
import renesca.parameter._
import renesca.parameter.implicits._

object ApiNodeFormat {
  implicit def LabelToString(label: Label): String = label.name

  private def tagLikeToSeq(tag: TagLike) = Seq(
    ("id", JsString(tag.uuid)),
    ("title", JsString(tag.title)),
    ("description", JsString(tag.description.getOrElse(""))),
    ("isClassification", JsBoolean(tag.isInstanceOf[Classification])),
    ("isContext", JsBoolean(tag.isInstanceOf[Scope])),
    ("color", JsNumber(tag.color)),
    ("symbol", tag.symbol.map(JsString(_)).getOrElse(JsNull))
  )

  implicit def tagWrites = new Writes[TagLike] {
    def writes(tag: TagLike) = JsObject(tagLikeToSeq(tag))
  }

  implicit def tagsWrites = new Writes[Tags] {
    def writes(cat: Tags) = cat.startNodeOpt.map(tag => JsObject(
      tagLikeToSeq(tag) ++ Seq(
        ("quality", cat.endNodeOpt.map(post => JsNumber(cat.quality(post.viewCount))).getOrElse(JsNull))
      )
    )).getOrElse(JsNull)
  }

  implicit object NodeFormat extends Format[Node] {
    def reads(json: JsValue) = ???

    //TODO: this should be multiple formats...code dup
    def writes(node: Node) = {
      JsObject(node match {
      case n: Post => Seq(
        ("id", JsString(n.uuid)),
        ("title", JsString(n.title)),
        ("description", JsString(n.description.getOrElse(""))),
        ("tags", JsArray(n.inRelationsAs(Tags).sortBy(_.uuid).map(tagsWrites.writes))), // TODO: why do we have to call tagsWrites.writes explicitly?
        //("classifications", JsArray(n.outRelationsAs(PostToConnects).flatMap(_.rev_classifies).map(tagWrites.writes))), //TODO WHY DOES THIS NOT WORK...
        ("classifications", {
          val discourse = Discourse(n.graph)
          JsArray(n.outRelationsAs(PostToConnects).map(_.endNode).flatMap(con => discourse.classifies.filter(_.endNode == con)).map(_.startNode).sortBy(_.uuid).map(tagWrites.writes))
        }),
        ("timestamp", Json.toJson(JsNumber(n.timestamp))),
        ("requestsEdit", Json.toJson(n.inRelationsAs(Updated))), //TODO: accessors for subrelations of hyperrelations, to get connected hypernode
        ("requestsTags", Json.toJson(n.inRelationsAs(AddTags) ++ n.inRelationsAs(RemoveTags))),
        ("viewCount", JsNumber(n.viewCount))
      )
      case n: Connects => Seq(
        ("id", JsString(n.uuid)),
        ("startId", n.startNodeOpt.map(s => JsString(s.uuid)).getOrElse(JsNull)),
        ("endId", n.endNodeOpt.map(e => JsString(e.uuid)).getOrElse(JsNull)),
        ("quality", n.startNodeOpt.map(post => JsNumber(n.quality(post.viewCount))).getOrElse(JsNull)),
        ("tags", Json.toJson(n.rev_classifies.sortBy(_.uuid).map(tagWrites.writes))) // TODO: why do we have to call tagsWrites.writes explicitly?
      )
      case n: TagLike => tagLikeToSeq(n) ++ (n match { //TODO: traits should have accessors for relations in magic
        case s:Scope => Seq(("inherits", JsArray(s.inherits.collect{ case t:TagLike => t }.map(tagWrites.writes))), //TODO inherits<Trait> methods in magic
          ("implements", JsArray(s.rev_inherits.collect{ case t:TagLike => t }.map(tagWrites.writes))))
        case c:Classification => Seq(("inherits", JsArray(c.inherits.collect{ case t:TagLike => t }.map(tagWrites.writes))),
          ("implements", JsArray(c.rev_inherits.collect{ case t:TagLike => t }.map(tagWrites.writes))))
      })
      case n: User        => Seq(
        ("id", JsString(n.uuid)),
        ("name", JsString(n.name)),
        ("email", JsString(n.email.getOrElse("")))
      )
      case n: Updated        => Seq(
        ("id", JsString(n.uuid)),
        ("oldTitle", JsString(n.oldTitle)),
        ("newTitle", JsString(n.newTitle)),
        ("oldDescription", JsString(n.oldDescription.getOrElse(""))),
        ("newDescription", JsString(n.newDescription.getOrElse(""))),
        ("vote", n.inRelationsAs(Votes).headOption.map(vote => JsObject(Seq(("weight", JsNumber(vote.weight))))).getOrElse(JsNull)),
        ("threshold", JsNumber(n.applyThreshold)),
        ("votes", JsNumber(n.approvalSum)),
        ("applied", JsBoolean(n.applied))
      )
      case n: AddTags    => Seq(
        ("id", JsString(n.uuid)),
        ("tags", Json.toJson(n.proposesTags.map(tagWrites.writes))),
        ("vote", n.inRelationsAs(Votes).headOption.map(vote => JsObject(Seq(("weight", JsNumber(vote.weight))))).getOrElse(JsNull)),
        ("threshold", JsNumber(n.applyThreshold)),
        ("votes", JsNumber(n.approvalSum)),
        ("isRemove", JsBoolean(false)),
        ("applied", JsBoolean(n.applied))
      )
      case n: RemoveTags    => Seq(
        ("id", JsString(n.uuid)),
        ("tags", Json.toJson(n.proposesTags.map(tagWrites.writes))),
        ("vote", n.inRelationsAs(Votes).headOption.map(vote => JsObject(Seq(("weight", JsNumber(vote.weight))))).getOrElse(JsNull)),
        ("threshold", JsNumber(n.applyThreshold)),
        ("votes", JsNumber(n.approvalSum)),
        ("isRemove", JsBoolean(true)),
        ("applied", JsBoolean(n.applied))
      )
      case n              =>
        throw new RuntimeException("You did not define a formatter for the api: " + node)
    })
    }
  }
}
