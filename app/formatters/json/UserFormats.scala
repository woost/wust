package formatters.json

import model.WustSchema.User
import play.api.libs.json._

/**
 * This object contains all format for User class
 */
object UserFormats {

  object RestFormat extends Format[User] {
    def reads(user: JsValue) = ???

    def writes(user: User) = JsObject(Seq(
      ("email", JsString(user.email.getOrElse(""))),
      ("id", JsString(user.uuid))
    ))
  }
}
