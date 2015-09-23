package formatters.json

import model.WustSchema._
import play.api.libs.json._
import renesca.graph.{Label, RelationType}
import renesca.schema._
import renesca.parameter._
import renesca.parameter.implicits._

object TagFormat {
  private def tagLikeToSeq(tag: TagLike) = Seq(
    ("id", JsString(tag.uuid)),
    ("title", JsString(tag.title)),
    ("description", JsString(tag.description.getOrElse(""))),
    ("isClassification", JsBoolean(tag.isInstanceOf[Classification])),
    ("isContext", JsBoolean(tag.isInstanceOf[Scope])),
    ("color", JsNumber(tag.color)),
    ("symbol", tag.symbol.map(JsString(_)).getOrElse(JsNull))
  )

  def karmaTagWriter(tag: Scope) = JsObject(
    tagLikeToSeq(tag) ++ Seq(
      ("karma", tag.inRelationsAs(HasKarma).headOption.map(has => JsNumber(has.karma)).getOrElse(JsNull))
    )
  )

  //TODO should user rev_Classifies accessor instead of getting tuple as parameter
  def classificationWriter(post: Post, tuple: (Classification, Seq[Connects])): JsObject = {
    val (classification, connectsList) = tuple
    val quality = connectsList.map(c => c.quality(post.viewCount)).sum / connectsList.size
    JsObject(
      tagLikeToSeq(classification) ++ Seq(
        ("quality", JsNumber(quality))
      )
    )
  }

  implicit object TagsFormat extends Format[Tags] {
    def reads(json: JsValue) = ???

    def writes(cat: Tags) = cat.startNodeOpt.map(tag => JsObject(
      tagLikeToSeq(tag) ++ Seq(
        ("quality", cat.endNodeOpt.map(post => JsNumber(cat.quality(post.viewCount))).getOrElse(JsNull)),
        ("vote", cat.inRelationsAs(Votes).headOption.map(vote => JsObject(Seq(("weight", JsNumber(vote.weight))))).getOrElse(JsNull)),
        ("classifications", JsArray(cat.rev_classifies.map(t => JsObject(tagLikeToSeq(t))))),
        ("tagsId", JsString(cat.uuid))
      )
    )).getOrElse(JsNull)
  }

  implicit object ScopeFormat extends Format[Scope] {
    def reads(json: JsValue) = ???

    def writes(s: Scope) = JsObject(tagLikeToSeq(s) ++ Seq(
      ("inherits", JsArray(s.inherits.collect{ case t:TagLike => t }.map(tag => JsObject(tagLikeToSeq(tag))))),
      ("implements", JsArray(s.rev_inherits.collect{ case t:TagLike => t }.map(tag => JsObject(tagLikeToSeq(tag)))))
    ))
  }

  implicit object ClassificationFormat extends Format[Classification] {
    def reads(json: JsValue) = ???

    def writes(c: Classification) = JsObject(tagLikeToSeq(c) ++ Seq(
      ("inherits", JsArray(c.inherits.collect{ case t:TagLike => t }.map(tag => JsObject(tagLikeToSeq(tag))))),
      ("implements", JsArray(c.rev_inherits.collect{ case t:TagLike => t }.map(tag => JsObject(tagLikeToSeq(tag)))))
    ))
  }
}
