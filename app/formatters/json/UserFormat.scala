package formatters.json

import model.WustSchema.{User, KarmaLog, LogOnScope}
import play.api.libs.json._
import formatters.json.TagFormat.tagLikeToSeq
import formatters.json.PostFormat.NodeFormat

object UserFormat {

  implicit object NodeFormat extends Format[User] {
    def reads(user: JsValue) = ???

    def writes(user: User) = JsObject(Seq(
      ("name", JsString(user.name)),
      ("id", JsString(user.uuid)),
      ("email", JsString(user.email.getOrElse("")))
    ))
  }

  private def logOnScopeWriter(rel: LogOnScope) = JsObject(tagLikeToSeq(rel.endNode) ++ Seq(
    ("currentKarma", JsNumber(rel.currentKarma))
  ))

  implicit object KarmaLogFormat extends Format[KarmaLog] {

    def reads(log: JsValue) = ???

    def writes(log: KarmaLog) = JsObject(Seq(
      ("karmaChange", JsNumber(log.karmaChange)),
      ("reason", JsString(log.reason)),
      ("timestamp", JsNumber(log.timestamp)),
      ("post", formatters.json.PostFormat.NodeFormat.writes(log.endNodeOpt.get)), //TODO: why need to call it explicitly
      ("contexts", JsArray(log.outRelationsAs(LogOnScope).map(logOnScopeWriter(_))))
    ))
  }
}
