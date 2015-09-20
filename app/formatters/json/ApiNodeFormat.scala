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

  def classificationWriter(post: Post, tuple: (Classification, Seq[Connects])): JsObject = {
    val (classification, connectsList) = tuple
    val quality = connectsList.map(c => c.quality(post.viewCount)).sum / connectsList.size
    JsObject(
      tagLikeToSeq(classification) ++ Seq(
        ("quality", JsNumber(quality))
      )
    )
  }

  implicit def tagsWrites = new Writes[Tags] {
    def writes(cat: Tags) = cat.startNodeOpt.map(tag => JsObject(
      tagLikeToSeq(tag) ++ Seq(
        ("quality", cat.endNodeOpt.map(post => JsNumber(cat.quality(post.viewCount))).getOrElse(JsNull)),
        ("vote", cat.inRelationsAs(Votes).headOption.map(vote => JsObject(Seq(("weight", JsNumber(vote.weight))))).getOrElse(JsNull))
      )
    )).getOrElse(JsNull)
  }

  implicit object NodeFormat extends Format[Node] {
    def reads(json: JsValue) = ???

    //TODO: this should be multiple formats...code dup
    def writes(node: Node) = JsObject(node match {
      case n: Post => Seq(
        ("id", JsString(n.uuid)),
        ("title", JsString(n.title)),
        ("description", JsString(n.description.getOrElse(""))),
        ("tags", JsArray(n.inRelationsAs(Tags).sortBy(_.uuid).map(tagsWrites.writes))), // TODO: why do we have to call tagsWrites.writes explicitly?
        ("classifications", JsArray(n.outRelationsAs(PostToConnects).map(_.endNode).flatMap(con => con.rev_classifies.sortBy(_.uuid).map((_, con))).groupBy(_._1).mapValues(_.map(_._2)).map(classificationWriter(n, _)).toSeq)),
        ("timestamp", Json.toJson(JsNumber(n.timestamp))),
        ("viewCount", JsNumber(n.viewCount))
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
    })
  }
}
