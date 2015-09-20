package formatters.json

import model.WustSchema.User
import play.api.libs.json._

object UserFormat {

  implicit object NodeFormat extends Format[User] {
    def reads(user: JsValue) = ???

    def writes(user: User) = JsObject(Seq(
      ("name", JsString(user.name)),
      ("id", JsString(user.uuid)),
      ("email", JsString(user.email.getOrElse("")))
    ))
  }
}
