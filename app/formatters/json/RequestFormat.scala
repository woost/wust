package formatters.json

import modules.requests._
import play.api.libs.json._
import play.api.libs.functional.syntax._

object RequestFormat {

  implicit val classificationFormat = (__ \ "id").read[String].map(ClassificationRequest(_))

  implicit val tagConnectFormat = (
    (__ \ "id").readNullable[String] and
      (__ \ "title").readNullable[String] and
      (__ \ "classifications").readNullable[List[ClassificationRequest]]
    )(TagConnectRequest)

  implicit val tagDisconnectFormat = (
    (__ \ "id").read[String] and
      (__ \ "classifications").readNullable[List[ClassificationRequest]]
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
      (__ \ "addedTags").readNullable[List[ClassificationRequest]] and
      (__ \ "removedTags").readNullable[List[ClassificationRequest]]
    )(ConnectsUpdateRequest)

  implicit val tagAddFormat = (__ \ "title").read[String].map(TagAddRequest(_))

  implicit val tagUpdateFormat = (__ \ "description").readNullable[String].map(TagUpdateRequest(_))

  implicit val userUpdateFormat = (__ \ "email").readNullable[String].map(UserUpdateRequest(_))
}
