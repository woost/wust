package formatters.json

import modules.requests._
import play.api.libs.json._
import play.api.libs.functional.syntax._

object RequestFormat {
  implicit val tagConnectFormat = (
    (__ \ "id").readNullable[String] and
      (__ \ "title").readNullable[String]
    )(TagConnectRequest)

  implicit val postAddFormat = (
    (__ \ "description").readNullable[String] and
      (__ \ "title").read[String] and
      (__ \ "addedTags").readNullable[List[TagConnectRequest]]
    )(PostAddRequest)

  implicit val postUpdateFormat = (
    (__ \ "description").readNullable[String] and
      (__ \ "title").readNullable[String] and
      (__ \ "addedTags").readNullable[List[TagConnectRequest]] and
      (__ \ "removedTags").readNullable[List[String]]
    )(PostUpdateRequest)

  implicit val connectableUpdateFormat = (
      (__ \ "addedTags").readNullable[List[TagConnectRequest]] and
      (__ \ "removedTags").readNullable[List[String]]
    )(ReferenceUpdateRequest)

  implicit val tagAddFormat = (__ \ "title").read[String].map(TagAddRequest(_))

  implicit val tagUpdateFormat = (__ \ "description").readNullable[String].map(TagUpdateRequest(_))

  implicit val userUpdateFormat = (__ \ "email").readNullable[String].map(UserUpdateRequest(_))
}
