package formatters.json

import model.WustSchema.User
import play.api.libs.json._

object UserFormats {

  object RestFormat extends Format[User] {
    def reads(user: JsValue) = ???

    def writes(user: User) = JsObject(Seq(
      ("name", JsString(user.name)),
      ("id", JsString(user.uuid))
    ))
  }
}
