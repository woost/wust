package formatters.json

import com.mohiva.play.silhouette.api.LoginInfo
import model.authorizations._
import model.users._
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
 * This object contains all format for User class
 */
object UserFormats {

  object RestFormat extends Format[User] {
    implicit val baseInfoFormat = BaseInfoFormats.restFormat

    implicit val reader = (
      (__ \ "id").read[String] ~
        (__ \ "loginInfo").read[LoginInfo] ~
        (__ \ "socials").readNullable(Reads.seq[LoginInfo]) ~
        (__ \ "email").readNullable(Reads.email) ~
        (__ \ "username").readNullable[String] ~
        (__ \ "info").read[BaseInfo] ~
        (__ \ "roles").readNullable(Reads.set[String]).map { case Some(r) => r.map(Role.apply) case None => Set[Role](SimpleUser) })(User.apply _)

    def reads(user: JsValue) = JsSuccess(user.as[User])

    def writes(user: User) = JsObject(Seq(
      ("info", Json.toJson(user.info)),
      ("id", JsString(user.id))
    ))
  }
}
