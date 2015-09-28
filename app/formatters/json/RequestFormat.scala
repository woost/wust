package formatters.json

import modules.requests._
import play.api.libs.json._
import play.api.libs.functional.syntax._

object RequestFormat {
  implicit val classificationConnectFormat = (
    (__ \ "id").readNullable[String] and
      (__ \ "title").readNullable[String]
    )(ClassificationConnectRequest)

  implicit val classificationDisconnectFormat = (__ \ "id").read[String].map(ClassificationDisconnectRequest(_))

  implicit val tagConnectFormat = (
    (__ \ "id").readNullable[String] and
      (__ \ "title").readNullable[String] and
      (__ \ "classifications").readNullable[List[ClassificationConnectRequest]]
    )(TagConnectRequest)

  implicit val tagDisconnectFormat = (
    (__ \ "id").read[String] and
      (__ \ "classifications").readNullable[List[ClassificationDisconnectRequest]]
    )(TagDisconnectRequest)

  implicit val postAddFormat = (
    (__ \ "description").readNullable[String] and
      (__ \ "title").read[String] and
      (__ \ "addedTags").readNullable[List[TagConnectRequest]]
    )(PostAddRequest)

  implicit val postUpdateFormat = (
    (__ \ "description").readNullable[String] and
      (__ \ "title").readNullable[String] and
      (__ \ "addedTags").readNullable[List[TagConnectRequest]] and
      (__ \ "removedTags").readNullable[List[TagDisconnectRequest]]
    )(PostUpdateRequest)

  implicit val connectableUpdateFormat = (
      (__ \ "addedTags").readNullable[List[ClassificationConnectRequest]] and
      (__ \ "removedTags").readNullable[List[ClassificationDisconnectRequest]]
    )(ConnectsUpdateRequest)

  implicit val tagAddFormat = (__ \ "title").read[String].map(TagAddRequest(_))

  implicit val tagUpdateFormat = (__ \ "description").readNullable[String].map(TagUpdateRequest(_))

  implicit val userUpdateFormat = (__ \ "email").readNullable[String].map(UserUpdateRequest(_))
}
